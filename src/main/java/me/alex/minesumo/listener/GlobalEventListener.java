package me.alex.minesumo.listener;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.ArenaPlayer;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.instances.ArenaImpl;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.events.PlayerJoinArenaEvent;
import me.alex.minesumo.events.PlayerLeaveArenaEvent;
import me.alex.minesumo.manager.MapManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.Optional;

public class GlobalEventListener {

    private final InstanceContainer container;

    public GlobalEventListener(Minesumo minesumo) {
        GlobalEventHandler gl = MinecraftServer.getGlobalEventHandler();

        container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setGenerator(unit -> unit.modifier().fillHeight(0, 2, Block.STONE));

        boolean isEditorMode = minesumo.getConfig().getIsInEditorMode();

        if (isEditorMode) gl.addListener(RemoveEntityFromInstanceEvent.class, removeEntityFromInstanceEvent -> {
            if (removeEntityFromInstanceEvent.getInstance() instanceof ArenaImpl arenaImpl
                    && removeEntityFromInstanceEvent.getEntity() instanceof Player player) {
                gl.call(new PlayerLeaveArenaEvent(arenaImpl, player));
            }
        });

        gl.addListener(PlayerLoginEvent.class, playerLoginEvent -> {
            playerLoginEvent.setSpawningInstance(container);

            if (isEditorMode) {
                playerLoginEvent.getPlayer().setGameMode(GameMode.CREATIVE);
                return;
            }

            Optional<MapConfig> mpf = minesumo.getMapManager().selectMap(MapManager.ALL_MAPS, MapManager.MapSelectionStrategy.ANY_RESULT);

            mpf.ifPresentOrElse(mapConfig -> {
                minesumo.getMapManager().getAvailableMap(mapConfig, ArenaImpl.ArenaState.WAITING_FOR_PLAYERS).whenComplete((arena, throwable) -> {
                    playerLoginEvent.getPlayer().setInstance(arena);
                });
            }, () -> playerLoginEvent.getPlayer().kick("No map found"));
        });

        gl.addListener(AddEntityToInstanceEvent.class, addEntityToInstanceEvent -> {
            if (addEntityToInstanceEvent.getInstance() instanceof ArenaImpl arenaImpl
                    && addEntityToInstanceEvent.getEntity() instanceof Player player) {
                PlayerJoinArenaEvent event = new PlayerJoinArenaEvent(arenaImpl, player);
                gl.call(event);
                addEntityToInstanceEvent.setCancelled(event.isCancelled());
            }
        });

        if (!isEditorMode) gl.addListener(PlayerMoveEvent.class, playerMoveEvent -> {
            if (!(playerMoveEvent.getInstance() instanceof ArenaImpl arenaImpl)) return;
            if (!(playerMoveEvent.getNewPosition().y() > arenaImpl.getMapConfig().getDeathLevel())) return;


            PlayerArenaDeathEvent death = new PlayerArenaDeathEvent(playerMoveEvent.getPlayer(), arenaImpl);
            gl.call(death);

            if (death.getNewPlayerPosition() != null) playerMoveEvent.setNewPosition(death.getNewPlayerPosition());
        });

        //Todo: Handle saving of player
        MinecraftServer.getConnectionManager().setPlayerProvider(ArenaPlayer::new);


        container.eventNode().addListener(PlayerMoveEvent.class, playerMoveEvent -> playerMoveEvent.setCancelled(true));
    }
}
