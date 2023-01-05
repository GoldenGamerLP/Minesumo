package me.alex.minesumo.map;

import dev.hypera.scaffolding.instance.SchematicChunkLoader;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.instances.MinesumoInstance;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.IChunkLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapCreator {
    private final ConcurrentMap<MapConfig, MinesumoInstance> activeMaps;
    private final ConcurrentMap<MapConfig, List<CompletableFuture<ArenaImpl>>> activeCalls;
    private final Pos pastePos = new Pos(0, 64, 0);

    public MapCreator(Minesumo minesumo) {
        this.activeMaps = new ConcurrentHashMap<>();
        this.activeCalls = new ConcurrentHashMap<>();

        minesumo.getSchematicLoader().getLoadedMapConfigs().forEach(mapConfig -> {
            MinesumoInstance instance = activeMaps.computeIfAbsent(mapConfig, mapConfig1 ->
                            new MinesumoInstance(mapConfig));

            IChunkLoader loader = SchematicChunkLoader.builder()
                    .addSchematic(mapConfig.getMapSchematic())
                    .saveHandler(chunk -> CompletableFuture.completedFuture(null))
                    .offset(pastePos.blockX(),pastePos.blockY(),pastePos.blockZ())
                    .build();

            instance.setChunkLoader(loader);
            System.out.println("Created map!");
        });
    }

    public CompletableFuture<MinesumoInstance> getEditorMap(MapConfig mapConfig) {
        return CompletableFuture.supplyAsync(() -> {
            MinesumoInstance instance = new MinesumoInstance(mapConfig);
            mapConfig.getMapSchematic().build(instance, pastePos).join();
            return instance;
        });
    }

    public ArenaImpl getMapNow(MapConfig config) {
        return this.activeMaps.get(config).createCopy();
    }


    public CompletableFuture<ArenaImpl> getMap(MapConfig mapConfig) {
        if (activeMaps.containsKey(mapConfig))
            return CompletableFuture
                    .completedFuture(activeMaps.get(mapConfig).createCopy());


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
        IChunkLoader loader = SchematicChunkLoader.builder()
                .addSchematic(mapConfig.getMapSchematic())
                .saveHandler(chunk -> CompletableFuture.completedFuture(null))
                .offset(pastePos.blockX(),pastePos.blockY(),pastePos.blockZ())
                .build();

        instance.setChunkLoader(loader);
        instance.scheduleNextTick(instance1 -> {
            activeCalls.get(mapConfig).forEach(queue -> queue.complete(instance.createCopy()));
            activeCalls.remove(mapConfig).clear();
        });

        //First call -> return shared instance
        return waitingCall;

    }
}
