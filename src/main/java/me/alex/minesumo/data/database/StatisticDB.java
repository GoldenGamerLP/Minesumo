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
import me.alex.minesumo.data.statistics.ArenaStatistics;
import me.alex.minesumo.data.statistics.PlayerStatistics;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import me.alex.minesumo.utils.MojangUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class StatisticDB {
    private static final Gson gson = JsonConfigurationLoader.getGson();

    private static final String
            arenaDBName = "arena-stats",
            playerDBName = "player-stats";


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

        RemovalListener<String, ArenaStatistics> removeArena = (key, value, cause) -> saveArena(key, value);
        AsyncCacheLoader<String, ArenaStatistics> loadArena = (key, cause) -> getOrCreateArenaStatistics(key);
        this.arenaCache = Caffeine.newBuilder()
                .removalListener(removeArena)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .executor(ForkJoinPool.commonPool())
                .buildAsync(loadArena);


        AsyncCacheLoader<UUID, PlayerStatistics> loadingPL = (key, executor) -> getPlayerStatistics(key);
        RemovalListener<UUID, PlayerStatistics> remPL = (key, value, cause) -> saveUser(key, value);
        this.playerCache = Caffeine.newBuilder()
                .removalListener(remPL)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .executor(ForkJoinPool.commonPool())
                .buildAsync(loadingPL);
    }

    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(UUID uuid) {
        Bson filter = playerFilter(uuid);

        return CompletableFuture.supplyAsync(() -> this.playerDB.find(filter)).thenApply(documents -> {
            Document dc = documents.first();
            if (dc == null) {
                //Used to get last known name (important because of rate-limiting)
                String name = MojangUtils.fromUuid(uuid.toString()).get("name").getAsString();
                return new PlayerStatistics(uuid, name);
            } else return gson.fromJson(dc.toBsonDocument().toJson(), PlayerStatistics.class);
        });
    }

    @NotNull
    public CompletableFuture<Map<UUID, PlayerStatistics>> getBulkPlayers(List<UUID> players) {
        CompletableFuture<Map<UUID, PlayerStatistics>> future = new CompletableFuture<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Map<UUID, PlayerStatistics> map = new ConcurrentHashMap<>();

        players.forEach(uuid ->
                futures.add(CompletableFuture.runAsync(() -> {
                    Document dc = this.playerDB.find(playerFilter(uuid)).first();
                    if (dc == null) map.put(uuid, null);
                    else map.put(uuid, gson.fromJson(dc.toBsonDocument().toJson(), PlayerStatistics.class));
                })));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> future.complete(map));
        return future;
    }

    @NotNull
    public CompletableFuture<PlayerStatistics> getPlayerStatistics(String name) {
        Bson filter = playerFilter(name);

        return CompletableFuture.supplyAsync(() -> this.playerDB.find(filter)).thenApply(documents -> {
            Document dc = documents.first();
            if (dc == null) {
                return null;
            } else return gson.fromJson(dc.toBsonDocument().toJson(), PlayerStatistics.class);
        });
    }

    private void saveUser(UUID player, PlayerStatistics stats) {
        Bson filter = playerFilter(player);

        CompletableFuture.supplyAsync(() -> this.playerDB.countDocuments(filter)).thenAccept(docs -> {
            Document json = Document.parse(gson.toJson(stats));
            if (docs == 0)
                this.playerDB.insertOne(json);
            else this.playerDB.replaceOne(filter, json);
        }).join();
    }

    private void saveArena(String gameID, ArenaStatistics stats) {
        CompletableFuture.runAsync(() -> this.arenaDB.insertOne(Document.parse(gson.toJson(stats)))).join();
    }

    @NotNull
    public CompletableFuture<ArenaStatistics> getOrCreateArenaStatistics(String gameID) {
        Bson filter = arenaFilter(gameID);

        return CompletableFuture.supplyAsync(() -> this.arenaDB.find(filter)).thenApply(documents -> {
            Document dc = documents.first();
            if (dc == null) return new ArenaStatistics(gameID);
            else return gson.fromJson(dc.toJson(), ArenaStatistics.class);
        });
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

    //Check if an arena exists
    @NotNull
    public CompletableFuture<Boolean> arenaExists(String gameID) {
        return CompletableFuture.supplyAsync(() -> this.arenaDB.countDocuments(arenaFilter(gameID)))
                .thenApply(count -> count != 0);
    }


    public Bson playerFilter(UUID uuid) {
        return eq("playerID", uuid.toString());
    }

    public Bson playerFilter(String name) {
        return eq("lastName", name);
    }

    public Bson arenaFilter(String uid) {
        return eq("sessionID", uid);
    }

}
