package me.alex.minesumo.listener;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.*;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.map.MapSelector;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GlobalEventListener {

    private final InstanceContainer container;

    public GlobalEventListener(Minesumo minesumo) {
        GlobalEventHandler gl = MinecraftServer.getGlobalEventHandler();

        container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.STONE));

        boolean isEditorMode = minesumo.getConfig().getIsInEditorMode();

        gl.addListener(RemoveEntityFromInstanceEvent.class, event -> {
            if (!(event.getInstance() instanceof ArenaImpl arenaImpl)) return;
            if (!(event.getEntity() instanceof Player player)) return;
            EventDispatcher.call(new PlayerLeaveArenaEvent(arenaImpl, player));
        });

        gl.addListener(PlayerLoginEvent.class, playerLoginEvent -> {
            Player player = playerLoginEvent.getPlayer();

            if (!minesumo.hasStarted()) {
                player.kick("Not loaded yet.");
                return;
            }


            if (isEditorMode) {
                player.setGameMode(GameMode.CREATIVE);
                playerLoginEvent.setSpawningInstance(container);
                return;
            }

            Optional<MapConfig> mpf = minesumo
                    .getMapManager()
                    .selectMap(MapSelector.ALL_MAPS, MapSelector.MapSelectionStrategy.RANDOM_RESULT);

            mpf.ifPresentOrElse(mapConfig -> {
                playerLoginEvent.setSpawningInstance(minesumo.getMapManager().test(mapConfig));
            }, () -> player.kick("No map found"));
        });

        gl.addListener(AddEntityToInstanceEvent.class, event -> {
            if (!(event.getInstance() instanceof ArenaImpl impl)) return;
            if (!(event.getEntity() instanceof Player player)) return;

            PlayerJoinArenaEvent ev = new PlayerJoinArenaEvent(impl, player);
            EventDispatcher.call(ev);
            event.setCancelled(event.isCancelled());

        });

        if (!isEditorMode) gl.addListener(PlayerMoveEvent.class, moveEvent -> {
            if (!(moveEvent.getInstance() instanceof ArenaImpl arenaImpl)) return;
            if (!(moveEvent.getNewPosition().y() <= arenaImpl.getMapConfig().getDeathLevel())) return;


            PlayerOutOfArenaEvent death = new PlayerOutOfArenaEvent(moveEvent.getPlayer(), arenaImpl);
            EventDispatcher.call(death);
            if (death.getNewPlayerPosition() != null) moveEvent.setNewPosition(death.getNewPlayerPosition());
        });

        gl.addListener(ArenaEndEvent.class, event -> {
            Instance instance = event.getInstance();
            Component component;

            if (event.getState() == ArenaEndEvent.EndState.WIN) {
                String player = event.getWinningPlayers()
                        .stream()
                        .map(Player::getUsername)
                        .toList()
                        .toString();

                component = Messages.GAME_WIN.toTranslatable(
                        Component.text(event.getTeamId()),
                        Component.text(player));

            } else component = Messages.GAME_DRAW.toTranslatable();

            instance.sendMessage(component);
        });

        gl.addListener(TeamEliminatedEvent.class, event -> {
            Instance instance = event.getInstance();
            Component component = Messages.GAME_TEAM_DEATH.toTranslatable(
                    Component.text(event.getTeamID()),
                    Component.text(event.getLastDeathPlayerOfTeam().getUsername()));

            instance.sendMessage(component);
        });

        gl.addListener(PlayerDeathEvent.class, event -> {
            ArenaImpl instance = (ArenaImpl) event.getInstance();
            String user = event.getPlayer().getUsername();

            Component component;
            if (event.getAttacker() != null)
                component = Messages.GAME_DEATH_PLAYER.toTranslatable(
                        Component.text(user),
                        Component.text(event.getAttacker().getUsername()));
            else component = Messages.GAME_DEATH
                    .toTranslatable(Component.text(user));

            instance.sendMessage(component);
        });


        if (!isEditorMode)
            container.eventNode().addListener(PlayerMoveEvent.class, playerMoveEvent -> playerMoveEvent.setCancelled(true));
    }
}
