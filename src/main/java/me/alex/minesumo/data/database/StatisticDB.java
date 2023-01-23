package me.alex.minesumo.data.database;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.entities.ArenaStatistics;
import me.alex.minesumo.data.entities.PlayerStatistics;
import me.alex.minesumo.utils.MojangUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.eq;

public class StatisticDB {
    private static final Gson gson = new Gson();

    private static final String
            arenaDBName = "arena-stats",
            playerDBName = "player-stats";
    private static final Bson ranking = new Document("$project", new Document()
            .append("lastName", "$lastName")
            .append("playerID", "$playerID")
            .append("ratio", new Document()
                    .append("$divide", Arrays.asList(
                            new Document("$cond", Arrays.asList(
                                    new Document("$eq", Arrays.asList("$kills", 0)),
                                    1,
                                    "$kills"
                            )),
                            new Document("$cond", Arrays.asList(
                                    new Document("$eq", Arrays.asList("$deaths", 0)),
                                    1,
                                    "$deaths"
                            ))
                    ))
            )
    );
    @Getter
    private final MongoCollection<Document> arenaDB;
    @Getter
    private final MongoCollection<Document> playerDB;
    @Getter
    private final AsyncLoadingCache<UUID, PlayerStatistics> playerCache;
    @Getter
    private final AsyncLoadingCache<String, ArenaStatistics> arenaCache;

    public StatisticDB(Minesumo minesumo) {
        MongoClient mongoDB = minesumo.getMongoDB().getMongoClient();

        MongoDatabase db = mongoDB.getDatabase("minesumo");

        this.arenaDB = db.getCollection(arenaDBName);
        this.playerDB = db.getCollection(playerDBName);

        AsyncCacheLoader<String, ArenaStatistics> loadArena = (key, cause) -> getOrCreateArenaStatistics(key);
        RemovalListener<String, ArenaStatistics> removeArena = (key, value, cause) -> saveArena(key, value);
        this.arenaCache = Caffeine.newBuilder()
                .removalListener(removeArena)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .executor(ForkJoinPool.commonPool())
                .buildAsync(loadArena);


        AsyncCacheLoader<UUID, PlayerStatistics> loadingPL = (key, executor) -> getPlayerStatistics(key);
        RemovalListener<UUID, PlayerStatistics> remPL = (key, value, cause) -> saveUser(key, value);
        this.playerCache = Caffeine.newBuilder()
                .removalListener(remPL)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .executor(ForkJoinPool.commonPool())
                .buildAsync(loadingPL);
    }

    public static Bson playerFilter(UUID uuid) {
        return eq("playerID", uuid.toString());
    }

    public static Bson playerFilter(String name) {
        return eq("lastName", name);
    }

    public static Bson arenaFilter(String uid) {
        return eq("sessionID", uid);
    }

    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(UUID uuid) {
        Bson filter = playerFilter(uuid);

        return CompletableFuture.supplyAsync(() -> this.getPlayerStats(filter, playerSupplier(uuid)));
    }

    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(String name) {
        Bson filter = playerFilter(name);

        return CompletableFuture.supplyAsync(() -> this.getPlayerStats(filter, () -> null));
    }

    @NotNull
    public CompletableFuture<Map<UUID, PlayerStatistics>> getBulkPlayers(List<UUID> players) {
        CompletableFuture<Map<UUID, PlayerStatistics>> future = new CompletableFuture<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<UUID, PlayerStatistics> map = new ConcurrentHashMap<>();

        //Optimize this to use cache if present
        for (UUID uuid : players) {
            CompletableFuture<Void> f = getPlayerStatistics(uuid).thenAccept(
                    playerStatistics -> map.put(uuid, playerStatistics)
            );
            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> future.complete(map));
        return future;
    }

    @Blocking
    @Nullable
    public PlayerStatistics getPlayerStats(Bson filter, @Nullable Supplier<PlayerStatistics> def) {
        Document dc = this.playerDB.find(filter).first();
        return getFromDocument(dc, def, PlayerStatistics.class);
    }

    @Blocking
    @Nullable
    public ArenaStatistics getArenaStats(Bson filter, @Nullable Supplier<ArenaStatistics> def) {
        Document dc = this.arenaDB.find(filter).first();
        return getFromDocument(dc, def, ArenaStatistics.class);
    }

    @NotNull
    public CompletableFuture<ArenaStatistics> getOrCreateArenaStatistics(String gameID) {
        Bson filter = arenaFilter(gameID);

        return CompletableFuture.supplyAsync(() -> getArenaStats(filter, arenaSupplier(gameID)));
    }

    public CompletableFuture<Long> getPlayerRanking(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = getPlayerCache().get(uuid).join();
            if (stats == null) return 0L;
            double kd = (stats.getKills() == 0 ? 1 : stats.getKills()) / (stats.getDeaths() == 0 ? 1D : stats.getDeaths());

            Bson sum = new Document("$group", new Document()
                    .append("_id", null)
                    .append("count", new Document("$sum", new Document("$cond", Arrays.asList(
                                    new Document("$lt", Arrays.asList("$ratio", kd)),
                                    1,
                                    0
                            )))
                    ));
            Document dc = this.playerDB.aggregate(Arrays.asList(ranking, sum)).first();
            //if dc is null return zero otherwise get dc #getLong("count")
            // +1 because the player is not included in the count
            return dc == null ? 0L : dc.getInteger("count").longValue() + 1L;
        });
    }

    public CompletableFuture<List<String>> getTopPlayers(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            Bson sort = new Document("$sort", new Document("ratio", -1));
            Bson limitation = new Document("$limit", limit);

            //Optimize this
            List<Document> documents = this.playerDB.aggregate(Arrays.asList(ranking, sort, limitation)).into(new ArrayList<>());
            List<String> stats = new LinkedList<>();
            documents.forEach(dc -> stats.add(dc.getString("lastName")));
            return stats;
        });
    }

    public CompletableFuture<Long> getPlayers() {
        return CompletableFuture.supplyAsync(this.playerDB::estimatedDocumentCount);
    }

    public CompletableFuture<Long> playedGames() {
        return CompletableFuture.supplyAsync(this.arenaDB::estimatedDocumentCount);
    }

    @Blocking
    private void saveUser(UUID player, PlayerStatistics stats) {
        Bson filter = playerFilter(player);

        Document json = Document.parse(gson.toJson(stats));
        if (this.playerDB.countDocuments(filter) == 0)
            this.playerDB.insertOne(json);
        else this.playerDB.replaceOne(filter, json);
    }

    @Blocking
    private void saveArena(String gameID, ArenaStatistics stats) {
        this.arenaDB.insertOne(Document.parse(gson.toJson(stats)));
    }

    @NotNull
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> this.playerDB.countDocuments(playerFilter(uuid)))
                .thenApply(count -> count != 0);
    }

    @NotNull
    public CompletableFuture<Boolean> playerExists(String name) {
        return CompletableFuture.supplyAsync(() -> this.playerDB.countDocuments(playerFilter(name)))
                .thenApply(count -> count != 0);
    }

    @NotNull
    public CompletableFuture<Boolean> arenaExists(String gameID) {
        return CompletableFuture.supplyAsync(() -> this.arenaDB.countDocuments(arenaFilter(gameID)))
                .thenApply(count -> count != 0);
    }

    public void editArenaStats(@NotNull String gameID, @NotNull Consumer<ArenaStatistics> stats) {
        this.getArenaCache().get(gameID).thenAccept(stats);
    }

    public void editPlayerStats(@NotNull UUID uuid, @NotNull Consumer<PlayerStatistics> stats) {
        this.getPlayerCache().get(uuid).thenAccept(stats);
    }

    public void editPlayerStats(@NotNull List<UUID> players, @NotNull Consumer<PlayerStatistics> stats) {
        this.getPlayerCache().getAll(players).thenAccept(map -> map.values().forEach(stats));
    }

    private <T> T getFromDocument(Document document, @Nullable Supplier<T> def, Class<T> clazz) {
        if (document == null) return def.get();
        return gson.fromJson(document.toJson(), clazz);
    }

    @Blocking
    private Supplier<PlayerStatistics> playerSupplier(UUID uuid) {
        String name = MojangUtils.fromUuid(uuid.toString()).get("name").getAsString();
        return () -> new PlayerStatistics(uuid, name);
    }

    @Blocking
    private Supplier<ArenaStatistics> arenaSupplier(String gameID) {
        return () -> new ArenaStatistics(gameID);
    }
}
