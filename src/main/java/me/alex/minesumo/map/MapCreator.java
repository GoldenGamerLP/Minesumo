package me.alex.minesumo.map;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.database.ArenaGameIDGenerator;
import me.alex.minesumo.data.entities.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.instances.MinesumoInstance;
import me.alex.minesumo.utils.SchematicUtils;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MapCreator {
    private final static Pos pastePos = new Pos(0, 64, 0);
    private final ConcurrentMap<MapConfig, MinesumoInstance> activeMaps;
    private final ConcurrentMap<MapConfig, List<CompletableFuture<ArenaImpl>>> activeCalls;
    private final ArenaGameIDGenerator idGenerator;

    public MapCreator(Minesumo minesumo) {
        this.activeMaps = new ConcurrentHashMap<>();
        this.activeCalls = new ConcurrentHashMap<>();
        this.idGenerator = minesumo.getGameIDGenerator();

        if (minesumo.getConfig().getFastJoin()) preLoadMaps(minesumo.getSchematicLoader());
    }

    private CompletableFuture<Void> preLoadMaps(SchematicHandler loader) {
        log.info("Starting pasting of schematics (Fast Join). This can take a while.");

        CompletableFuture<Long> time = CompletableFuture.completedFuture(System.currentTimeMillis());
        List<CompletableFuture<?>> calls = new ArrayList<>();

        loader.getLoadedMapConfigs().forEach(mapConfig -> {
            MinesumoInstance instance = activeMaps
                    .computeIfAbsent(mapConfig,
                            mapConfig1 -> new MinesumoInstance(mapConfig));

            calls.add(SchematicUtils.pasteSchematic(mapConfig.getMapSchematic(), instance, pastePos));
        });

        return CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).thenRun(() -> {
            long ms = System.currentTimeMillis() - time.join();
            log.info("Done Pre-Loading Maps and Schematics. In {} ms", ms);
        });
    }

    public CompletableFuture<MinesumoInstance> getEditorMap(MapConfig mapConfig) {
        return CompletableFuture.supplyAsync(() -> {
            MinesumoInstance instance = new MinesumoInstance(mapConfig);
            SchematicUtils.pasteSchematic(mapConfig.getMapSchematic(), instance, pastePos).join();
            return instance;
        });
    }


    public CompletableFuture<ArenaImpl> getMap(MapConfig mapConfig) {
        if (activeMaps.containsKey(mapConfig))
            return idGenerator.getSafeArenaUID()
                    .thenApply(gameID -> activeMaps.get(mapConfig).createCopy(gameID));


        //Putting a call to the queue, which will later be completed
        if (activeCalls.containsKey(mapConfig)) {
            CompletableFuture<ArenaImpl> waitingCall = new CompletableFuture<>();

            activeCalls.computeIfPresent(mapConfig, (mapConfig1, completableFutures) -> {
                completableFutures.add(waitingCall);
                return completableFutures;
            });

            return waitingCall;
        }


        //Map was not initialized before

        //Creating default instance
        MinesumoInstance instance = activeMaps
                .computeIfAbsent(mapConfig, mapConfig1 ->
                        new MinesumoInstance(mapConfig));

        CompletableFuture<ArenaImpl> waitingCall = new CompletableFuture<>();

        //Creating queue for other instances
        this.activeCalls.put(mapConfig, new ArrayList<>(List.of(waitingCall)));

        //Pasting schematic, after pasted complete every request and give back shared instance
        CompletableFuture<Void> paste = SchematicUtils.pasteSchematic(mapConfig.getMapSchematic(), instance, pastePos);

        paste.thenAccept(region -> {
            activeCalls.get(mapConfig).forEach(
                    queue -> idGenerator.getSafeArenaUID().thenAccept(
                            s -> queue.complete(instance.createCopy(s))));

            activeCalls.remove(mapConfig).clear();
        });

        //First call -> return shared instance
        return waitingCall;

    }
}
