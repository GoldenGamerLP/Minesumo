package me.alex.minesumo.manager;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.instances.Arena;
import me.alex.minesumo.data.instances.MinesumoInstance;
import me.alex.minesumo.utils.DefaultInstanceSettings;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.SharedInstance;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapCreator {

    private final ConcurrentMap<MapConfig, MinesumoInstance> activeMaps;
    private final ConcurrentMap<MapConfig, List<CompletableFuture<Arena>>> activeCalls;
    private final @NotNull InstanceManager instanceManager;
    private final Pos pastePos = new Pos(0, 64, 0);

    public MapCreator(Minesumo minesumo) {
        this.activeMaps = new ConcurrentHashMap<>();
        this.activeCalls = new ConcurrentHashMap<>();
        this.instanceManager = MinecraftServer.getInstanceManager();
    }


    public CompletableFuture<Arena> getMap(MapConfig mapConfig) {
        if (activeMaps.containsKey(mapConfig))
            return CompletableFuture
                    .completedFuture(activeMaps.get(mapConfig).createCopy());


        //Putting a call to the queue, which will later be completed
        if (activeCalls.containsKey(mapConfig)) {
            CompletableFuture<Arena> waitingCall = new CompletableFuture<>();

            activeCalls.computeIfPresent(mapConfig,(mapConfig1, completableFutures) -> {
                completableFutures.add(waitingCall);
                return completableFutures;
            });

            return waitingCall;
        }

        return CompletableFuture.supplyAsync(() -> {
            //Map was not initialized before

            //Creating default instance
            MinesumoInstance instance = activeMaps
                    .computeIfAbsent(mapConfig, mapConfig1 ->
                            new MinesumoInstance(mapConfig));

            //Creating queue for other instances
            this.activeCalls.put(mapConfig, new ArrayList<>());

            //Pasting schematic, after pasted complete every request and give back shared instance
            mapConfig.getMapSchematic().build(instance, pastePos).thenAcceptAsync(region -> {
                activeCalls.get(mapConfig).forEach(queue -> queue.complete(instance.createCopy()));
                activeCalls.remove(mapConfig).clear();
            });

            //First call -> return shared instance
            return instance.createCopy();
        });
    }
}
