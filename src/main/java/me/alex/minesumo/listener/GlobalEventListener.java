package me.alex.minesumo.listener;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.statistics.PlayerStatistics;
import me.alex.minesumo.data.statistics.StatisticsManager;
import me.alex.minesumo.events.ArenaChangeStateEvent;
import me.alex.minesumo.events.ArenaEndEvent;
import me.alex.minesumo.events.PlayerDeathEvent;
import me.alex.minesumo.events.TeamEliminatedEvent;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.map.MapSelector;
import me.alex.minesumo.messages.Messages;
import me.alex.minesumo.utils.ListUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GlobalEventListener {

    private final InstanceContainer container;

    public GlobalEventListener(Minesumo minesumo) {
        StatisticsManager statsMng = minesumo.getStatisticsManager();
        GlobalEventHandler gl = MinecraftServer.getGlobalEventHandler();

        container = MinecraftServer.getInstanceManager().createInstanceContainer();
        container.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.STONE));

        boolean isEditorMode = minesumo.getConfig().getIsInEditorMode();

        gl.addListener(PlayerLoginEvent.class, playerLoginEvent -> {
            Player player = playerLoginEvent.getPlayer();

            if (!minesumo.hasStarted()) {
                player.kick("Not loaded yet.");
                return;
            }

            playerLoginEvent.setSpawningInstance(container);

            if (isEditorMode) {
                player.setGameMode(GameMode.CREATIVE);
                return;
            }

            Optional<MapConfig> mpf = minesumo
                    .getMapManager()
                    .selectMap(MapSelector.ALL_MAPS, MapSelector.MapSelectionStrategy.RANDOM_RESULT);

            mpf.ifPresentOrElse(mapConfig -> {
                CompletableFuture<ArenaImpl> instance = minesumo.getMapManager()
                        .getAvailableMap(
                                mapConfig,
                                ArenaImpl.ArenaState.WAITING_FOR_PLAYERS);

                //For Performance reasons
                if (instance.isDone()) playerLoginEvent.setSpawningInstance(instance.join());
                else instance.thenAccept(arena -> minesumo.getMapManager().queueArena(player, arena));
            }, () -> player.kick("No map found"));
        });

        gl.addListener(ArenaEndEvent.class, event -> {
            ArenaImpl instance = (ArenaImpl) event.getInstance();
            String player = ListUtils.formatList(event.getWinningPlayers(), Player::getUsername);
            Component component;

            if (event.getState() == ArenaEndEvent.EndState.WIN) {

                component = Messages.GAME_WIN.toTranslatable(
                        Component.text(event.getTeamId()),
                        Component.text(player));

            } else component = Messages.GAME_DRAW.toTranslatable();

            statsMng.arenaEnd(instance.getGameID(), event.getState(), event.getWinningPlayers());
            instance.getPlayers(ArenaImpl.PlayerState.ALIVE).forEach(pls -> minesumo.getStatsHandler()
                    .getPlayerCache()
                    .getIfPresent(pls.getUuid()).thenAccept(pl -> {

                        int kills = pl.getKills();
                        int deaths = pl.getDeaths();
                        double kd = kills * 1D / deaths;
                        int team = instance.getPlayersTeamIds().getOrDefault(pls.getUuid(), -1);
                        pls.sendMessage(Messages.GAME_END_SUMMARY_SELF.toTranslatable(
                                Component.text(kills),
                                Component.text(deaths),
                                Component.text(String.format("%1.2f", kd)),
                                Component.text(team)));
                    }));

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

            //Stats
            UUID deathID = event.getAttacker() == null ? null : event.getAttacker().getUuid();
            statsMng.addDeath(instance.getGameID(), event.getPlayer().getUuid(), deathID);
        });

        gl.addListener(ArenaChangeStateEvent.class, event -> {
            ArenaImpl impl = event.getArena();
            if (event.getState() == ArenaImpl.ArenaState.NORMAL_STARTING) {
                statsMng.startGame(impl.getGameID(), impl.getPlayers(ArenaImpl.PlayerState.ALIVE));
            } else if (event.getState() == ArenaImpl.ArenaState.ENDING) {
                minesumo.getStatsHandler().getArenaCache().synchronous().invalidate(impl.getGameID());
            }
        });

        gl.addListener(AsyncPlayerPreLoginEvent.class, event -> {
            PlayerStatistics stats = minesumo.getStatsHandler().getPlayerCache()
                    .get(event.getPlayerUuid())
                    .join();

            //Update name
            stats.setLastName(event.getUsername());
        });

        gl.addListener(PlayerDisconnectEvent.class, event -> {
            minesumo.getStatsHandler()
                    .getPlayerCache()
                    .synchronous()
                    .invalidate(event.getPlayer().getUuid());
        });

        if (!isEditorMode)
            container.eventNode().addListener(PlayerMoveEvent.class, playerMoveEvent -> playerMoveEvent.setCancelled(true));
    }
}
