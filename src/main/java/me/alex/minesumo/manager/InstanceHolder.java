package me.alex.minesumo.manager;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
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

public class InstanceHolder {

    private final ConcurrentMap<MapConfig, InstanceContainer> activeMaps;
    private final ConcurrentMap<MapConfig, List<CompletableFuture<SharedInstance>>> activeCalls;
    private final Minesumo minesumo;
    private final @NotNull InstanceManager instanceManager;

    private final Pos pastePos = new Pos(0, 64, 0);

    public InstanceHolder(Minesumo minesumo) {
        this.minesumo = minesumo;
        this.activeMaps = new ConcurrentHashMap<>();
        this.activeCalls = new ConcurrentHashMap<>();
        this.instanceManager = MinecraftServer.getInstanceManager();
    }


    public CompletableFuture<SharedInstance> getMap(MapConfig mapConfig) {
        if (activeMaps.containsKey(mapConfig))
            return CompletableFuture
                    .completedFuture(instanceManager
                            .createSharedInstance(activeMaps.get(mapConfig)));


        //Putting a call to the queue, which will later be completed
        if (activeCalls.containsKey(mapConfig)) {
            List<CompletableFuture<SharedInstance>> queue = activeCalls.getOrDefault(mapConfig, new ArrayList<>());
            CompletableFuture<SharedInstance> waitingCall = new CompletableFuture<>();
            activeCalls.put(mapConfig, queue);
            return waitingCall;
        }
        return CompletableFuture.supplyAsync(() -> {
            //Map was not initialized before

            //Creating default instance
            InstanceContainer instance = activeMaps
                    .computeIfAbsent(mapConfig, mapConfig1 ->
                            instanceManager.createInstanceContainer(DefaultInstanceSettings.DEFAULT_DIM));

            //Creating queue for other instances
            this.activeCalls.put(mapConfig, new ArrayList<>());

            //Pasting schematic, after pasted complete every request and give back shared instance
            mapConfig.getMapSchematic().build(instance, pastePos).thenAcceptAsync(region -> {
                activeCalls.get(mapConfig).forEach(queue -> {
                    queue.complete(instanceManager.createSharedInstance(instance));
                });
                activeMaps.remove(mapConfig);
            });

            //First call -> return shared instance
            return instanceManager.createSharedInstance(instance);
        });
    }
}
