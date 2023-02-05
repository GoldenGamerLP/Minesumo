package me.alex.minesumo.listener;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.entities.PlayerStatistics;
import me.alex.minesumo.data.statistics.StatisticsManager;
import me.alex.minesumo.events.*;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import me.alex.minesumo.tablist.TabManager;
import me.alex.minesumo.tablist.TablistPlayerChangeEvent;
import me.alex.minesumo.utils.ColorUtils;
import me.alex.minesumo.utils.ListUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.List;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;

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

            minesumo.getMapSelection().addPlayersToQueue(List.of(player));
        });

        gl.addListener(ArenaEndEvent.class, event -> {
            ArenaImpl instance = (ArenaImpl) event.getInstance();
            String player = ListUtils.formatList(event.getWinningPlayers(), Player::getUsername);
            Component component;

            if (event.getState() == ArenaEndEvent.EndState.WIN) {

                component = Messages.GAME_WIN.toTranslatable(
                        text(event.getTeamId()),
                        text(player));

            } else component = Messages.GAME_DRAW.toTranslatable();

            statsMng.arenaEnd(instance.getGameID(), event.getState(), event.getWinningPlayers());
            instance.getPlayers().forEach(pls -> minesumo.getStatsHandler()
                    .getPlayerCache()
                    //Why NPE? I have no idea.
                    .getIfPresent(pls.getUuid()).thenAccept(pl -> {

                        int kills = pl.getKills();
                        int deaths = pl.getDeaths();
                        double kd = kills * 1D / deaths;
                        int team = instance.getPlayersTeamIds().getOrDefault(pls.getUuid(), -1);
                        pls.sendMessage(Messages.GAME_END_SUMMARY_SELF.toTranslatable(
                                text(kills),
                                text(deaths),
                                text(String.format("%1.2f", kd)),
                                text(team)));
                    }));

            instance.sendMessage(component);
        });

        gl.addListener(TeamEliminatedEvent.class, event -> {
            Instance instance = event.getInstance();
            Component component = Messages.GAME_TEAM_DEATH.toTranslatable(
                    text(event.getTeamID()),
                    text(event.getLastDeathPlayerOfTeam().getUsername()));

            //Set Tab to Spectator
            TablistPlayerChangeEvent change = new TablistPlayerChangeEvent(event.getLastDeathPlayerOfTeam(), teamBuilder -> {
                teamBuilder.withColor(NamedTextColor.GRAY);
                teamBuilder.withPrefix(Component.text("[Spectator] ").color(NamedTextColor.GRAY));
                teamBuilder.withPriority(0);
            });


            EventDispatcher.call(change);

            instance.sendMessage(component);
        });

        gl.addListener(PlayerDeathEvent.class, event -> {
            ArenaImpl instance = (ArenaImpl) event.getInstance();
            String user = event.getPlayer().getUsername();
            Integer team = event.getTeamId();

            Component component;
            if (event.getAttacker() != null)
                component = Messages.GAME_DEATH_PLAYER.toTranslatable(
                        text(user),
                        text(event.getAttacker().getUsername()));
            else component = Messages.GAME_DEATH
                    .toTranslatable(text(user));

            instance.sendMessage(component);

            //Remove PVP Tag
            event.getPlayer().removeTag(PvPEvents.LAST_HIT);

            //Stats
            UUID deathID = event.getAttacker() == null ? null : event.getAttacker().getUuid();
            statsMng.addDeath(instance.getGameID(), event.getPlayer().getUuid(), deathID);
        });

        gl.addListener(ArenaChangeStateEvent.class, event -> {
            ArenaImpl impl = event.getArena();
            if (event.getState() == ArenaImpl.ArenaState.NORMAL_STARTING) {
                statsMng.startGame(impl.getGameID(), impl.getPlayerFromState(ArenaImpl.PlayerState.ALIVE));

                //Set Tab for alive player to their team + new color

            } else if (event.getState() == ArenaImpl.ArenaState.ENDING) {
                minesumo.getStatsHandler().getArenaCache().synchronous().invalidate(impl.getGameID());
            } else if (event.getState() == ArenaImpl.ArenaState.INGAME) {
                //A list of alive players
                Integer[] lifes = impl.getLives();
                List<Integer> teams = impl.getLivingTeams();

                for (Integer team : teams) {
                    List<Player> pls = impl.getPlayersFromTeam(team);

                    statsMng.addTeam(impl.getGameID(), team, pls);

                    for (Player pl : pls) {
                        TablistPlayerChangeEvent change = new TablistPlayerChangeEvent(pl, teamBuilder -> {
                            teamBuilder.withPrefix(Component.text("[" + team + "] ").color(NamedTextColor.GRAY));
                            teamBuilder.withColor(NamedTextColor.nearestTo(TextColor.color(ColorUtils.getColorFromInt(team).getRGB())));
                            teamBuilder.withPriority(team + 1);
                        });

                        EventDispatcher.call(change);
                    }
                }
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

            TabManager.resetPlayer(event.getPlayer());
        });

        gl.addListener(PlayerLeaveArenaEvent.class, event -> {
            TabManager.resetPlayer(event.getPlayer());
        });

        if (!isEditorMode)
            container.eventNode().addListener(PlayerMoveEvent.class, playerMoveEvent -> playerMoveEvent.setCancelled(true));
    }
}
