package me.alex.minesumo.data.database;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.expressions.MathExpressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import dev.morphia.aggregation.experimental.stages.Sort;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.experimental.filters.Filter;
import dev.morphia.query.experimental.filters.Filters;
import lombok.Getter;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.enities.ArenaStatistics;
import me.alex.minesumo.data.enities.PlayerStatistics;
import me.alex.minesumo.utils.MojangUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

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
            ));
    @Getter
    private final MongoCollection<Document> arenaDB;
    @Getter
    private final MongoCollection<Document> playerDB;
    @Getter
    private final Datastore mongoDBStorage;
    @Getter
    private final AsyncLoadingCache<UUID, PlayerStatistics> playerCache;
    @Getter
    private final AsyncLoadingCache<String, ArenaStatistics> arenaCache;

    public StatisticDB(Minesumo minesumo) {
        MongoClient mongoDB = minesumo.getMongoDB().getMongoClient();

        MongoDatabase db = mongoDB.getDatabase("minesumo");

        this.mongoDBStorage = Morphia.createDatastore(mongoDB, "minesumo");

        this.arenaDB = db.getCollection(arenaDBName);
        this.playerDB = db.getCollection(playerDBName);

        RemovalListener<String, ArenaStatistics> removeArena = (key, value, cause) -> saveArena(key, value);
        AsyncCacheLoader<String, ArenaStatistics> loadArena = (key, cause) -> getOrCreateArenaStatistics(key);
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

    public static Filter playerFilter(UUID uuid) {
        return Filters.eq("playerID", uuid);
    }

    public static Filter playerFilter(String name) {
        return Filters.eq("lastName", name);
    }

    public static Filter arenaFilter(String uid) {
        return Filters.eq("gameID", uid);
    }

    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(UUID uuid) {
        Filter filter = playerFilter(uuid);

        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = mongoDBStorage.find(PlayerStatistics.class)
                    .filter(filter)
                    .first();

            if (stats == null) {
                String lastName = MojangUtils.fromUuid(uuid.toString()).get("name").getAsString();
                stats = new PlayerStatistics(uuid, lastName);
            }

            return stats;
        });
    }

    /**
     * Returns the Playerstatistics for the given name. <b>Case-Sensitive!</b>
     *
     * @param name The name of the player
     * @return {@link PlayerStatistics} for the given name
     */
    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(String name) {
        Filter filter = playerFilter(name);

        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = mongoDBStorage.find(PlayerStatistics.class)
                    .filter(filter)
                    .first();

            return stats;
        });
    }

    @NotNull
    public CompletableFuture<Map<UUID, PlayerStatistics>> getBulkPlayers(List<UUID> players) {
        CompletableFuture<Map<UUID, PlayerStatistics>> future = new CompletableFuture<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<UUID, PlayerStatistics> map = new ConcurrentHashMap<>();

        players.forEach(uuid -> futures.add(playerCache.get(uuid).thenAccept(stats -> map.put(uuid, stats))));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> future.complete(map));
        return future;
    }

    public CompletableFuture<Long> getPlayerRanking(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = getPlayerCache().get(uuid).join();
            if (stats == null) return 0L;
            double kd = (stats.getKills() == 0 ? 1 : stats.getKills()) / (stats.getDeaths() == 0 ? 1D : stats.getDeaths());

            MorphiaCursor<Document> query = mongoDBStorage.aggregate(PlayerStatistics.class)
                    .match(Filters.and(Filters.gt("kills", 0), Filters.gt("deaths", 0)))
                    .project(Projection.project().include("ratio", MathExpressions.divide(Expressions.field("kills"), Expressions.field("deaths"))))
                    .sort(Sort.sort().descending("ratio"))
                    .match(Filters.gte("ratio", kd))
                    .count("count")
                    .execute(Document.class);

            Document dc = query.tryNext();
            query.close();
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
        this.mongoDBStorage.save(stats);
    }

    private void saveArena(String gameID, ArenaStatistics stats) {
        this.mongoDBStorage.save(stats);
    }

    @NotNull
    public CompletableFuture<ArenaStatistics> getOrCreateArenaStatistics(String gameID) {
        Filter filter = arenaFilter(gameID);

        return CompletableFuture.supplyAsync(() -> {
            ArenaStatistics stats = mongoDBStorage.find(ArenaStatistics.class)
                    .filter(filter)
                    .first();

            if (stats == null) {
                stats = new ArenaStatistics(gameID);
            }

            return stats;
        });
    }

    @NotNull
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = mongoDBStorage.find(PlayerStatistics.class)
                    .filter(playerFilter(uuid))
                    .first();

            return stats != null;
        });
    }

    @NotNull
    public CompletableFuture<Boolean> playerExists(String name) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStatistics stats = mongoDBStorage.find(PlayerStatistics.class)
                    .filter(playerFilter(name))
                    .first();

            return stats != null;
        });
    }

    //Check if an arena exists
    @NotNull
    public CompletableFuture<Boolean> arenaExists(String gameID) {
        return CompletableFuture.supplyAsync(() -> {
            ArenaStatistics stats = mongoDBStorage.find(ArenaStatistics.class)
                    .filter(arenaFilter(gameID))
                    .first();

            return stats != null;
        });
    }

}
