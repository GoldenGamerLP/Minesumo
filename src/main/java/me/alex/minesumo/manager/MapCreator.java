package me.alex.minesumo.manager;

import io.github.bloepiloepi.pvp.config.AttackConfig;
import io.github.bloepiloepi.pvp.config.ExplosionConfig;
import io.github.bloepiloepi.pvp.config.PvPConfig;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.instances.ArenaImpl;
import me.alex.minesumo.data.instances.MinesumoInstance;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityInstanceEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MapCreator {

    private final ConcurrentMap<MapConfig, MinesumoInstance> activeMaps;
    private final ConcurrentMap<MapConfig, List<CompletableFuture<ArenaImpl>>> activeCalls;
    private final Pos pastePos = new Pos(0, 64, 0);
    private final EventNode<EntityInstanceEvent> pvpEventNode;

    public MapCreator(Minesumo minesumo) {
        this.activeMaps = new ConcurrentHashMap<>();
        this.activeCalls = new ConcurrentHashMap<>();

        this.pvpEventNode = PvPConfig.emptyBuilder()
                .attack(AttackConfig
                        .emptyBuilder(false)
                        .legacyKnockback(true)
                        .sounds(true)
                        .attackCooldown(false)
                        .damageIndicatorParticles(true)
                        .spectating(true)
                ).explosion(ExplosionConfig.DEFAULT)
                .build().createNode();

    }

    public CompletableFuture<MinesumoInstance> getEditorMap(MapConfig mapConfig) {
        return CompletableFuture.supplyAsync(() -> {
            MinesumoInstance instance = new MinesumoInstance(mapConfig);
            mapConfig.getMapSchematic().build(instance, pastePos).join();
            return instance;
        });
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

        //PVP events
        instance.eventNode().addChild(this.pvpEventNode);

        //Creating queue for other instances
        this.activeCalls.put(mapConfig, new ArrayList<>(List.of(waitingCall)));

        //Pasting schematic, after pasted complete every request and give back shared instance
        mapConfig.getMapSchematic().build(instance, pastePos).thenAcceptAsync(region -> {
            activeCalls.get(mapConfig).forEach(queue -> queue.complete(instance.createCopy()));
            activeCalls.remove(mapConfig).clear();
        });

        //First call -> return shared instance
        return waitingCall;

    }
}
