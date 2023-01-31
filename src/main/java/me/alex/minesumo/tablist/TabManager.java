package me.alex.minesumo.tablist;


import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Use this class to manage the tablist.
 * <p>
 * To change a nametag use event. For example:
 * <pre> TablistPlayerChangeEvent event = new TablistPlayerChangeEvent(player, prefix, suffix, color);
 * EventDispatcher.call(event);</pre>
 *
 * <b>This event can also be fired async. Existing teams cannot be updated.</b>
 *
 * @author Alex
 * @version 1.0
 * @since 2023-01-23
 */
public final class TabManager {

    private static final TeamManager teamManager = MinecraftServer.getTeamManager();
    private static final Scheduler scheduler = MinecraftServer.getSchedulerManager();
    private static final Map<Integer, Team> hashComponents = new HashMap<>();
    private static final Map<UUID, Integer> playerTeams = new ConcurrentHashMap<>(6);
    private static final EventNode<PlayerEvent> NODE = EventNode.type("minesumo:tablist", EventFilter.PLAYER);
    private static BiConsumer<Player, me.alex.minesumo.tablist.Team.TeamBuilder> defPrefix = (player, teamBuilder) -> teamBuilder.withColor(NamedTextColor.GRAY);

    static {
        //Prefix on player spawn event
        NODE.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();

            //Run async because of the database
            scheduler.scheduleNextTick(() -> {
                me.alex.minesumo.tablist.Team.TeamBuilder builder = me.alex.minesumo.tablist.Team.TeamBuilder.newBuilder();
                defPrefix.accept(player, builder);
                me.alex.minesumo.tablist.Team team = builder.build();

                TablistPlayerChangeEvent tabChangeEvent = new TablistPlayerChangeEvent(player, team.getPrefix(), team.getSuffix(), team.getColor());
                EventDispatcher.call(tabChangeEvent);
            }, ExecutionType.ASYNC);
        });

        NODE.addListener(TablistPlayerChangeEvent.class, event -> {
            Player player = event.getPlayer();
            int hash = event.getPrefix().hashCode() + event.getSuffix().hashCode();

            //Is in the same team
            if (playerTeams.containsKey(player.getUuid()) && playerTeams.get(player.getUuid()) == hash)
                return;


            //Register the team
            registerTeam(hash + "", builder -> {
                builder.prefix(event.getPrefix());
                builder.suffix(event.getSuffix());
                builder.teamColor(event.getColor());
                builder.collisionRule(TeamsPacket.CollisionRule.NEVER);
                builder.updateNameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS);
                builder.seeInvisiblePlayers();
            });

            //Update the player
            updatePlayer(player, hash + "");

            //Update the player team
            playerTeams.put(player.getUuid(), hash);

            //Update the hashComponents
            hashComponents.put(hash, teamManager.getTeam(hash + ""));
        });

        //On player disconnect server event. Remove the player from the team
        NODE.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            removePlayer(player);
        });
    }

    private TabManager() {
    }

    /**
     * Gets the event node to register events regarding the Prefix/Suffix/Color Nametags
     *
     * @return the event node
     */
    public static EventNode<PlayerEvent> getNode() {
        return NODE;
    }

    /**
     * Registers a team if it is not already registered
     *
     * @param team        the team name
     * @param teamBuilder the team builder
     */
    private static void registerTeam(String team, Consumer<TeamBuilder> teamBuilder) {
        if (teamManager.getTeam(team) == null) {
            TeamBuilder builder = teamManager.createBuilder(team);
            teamBuilder.accept(builder);
            //Registers the team or updates it
            builder.build();
        }
    }

    /**
     * Sets the default prefix for every individual player
     *
     * @param prefix the prefix
     */
    public static void defaultPrefix(BiConsumer<Player, me.alex.minesumo.tablist.Team.TeamBuilder> prefix) {
        defPrefix = prefix;
    }

    /**
     * Updates the player with the team name
     *
     * @param player   the player
     * @param teamName the team name
     */
    private static void updatePlayer(Player player, String teamName) {
        Team team1 = teamManager.getTeam(teamName);
        Integer team2 = playerTeams.get(player.getUuid());
        Team firstTeam = team2 == null ? null : hashComponents.get(team2);


        if (team1 == null) {
            return;
        }
        if (firstTeam != null) {
            firstTeam.removeMember(player.getUsername());
            //Only remove from hashComponents if the team is empty
            if (firstTeam.getMembers().isEmpty()) {
                hashComponents.remove(team2);
            }
        }

        team1.addMember(player.getUsername());
        //Update the players team with the hashcode
        playerTeams.put(player.getUuid(), team1.getPrefix().hashCode() + team1.getSuffix().hashCode());
    }

    /**
     * Removes the player from the team
     *
     * @param player the player
     */
    private static void removePlayer(Player player) {
        Integer team = playerTeams.remove(player.getUuid());
        Team team2 = team == null ? null : hashComponents.get(team);
        if (team2 != null) {
            team2.removeMember(player.getUsername());
            //Only remove from hashComponents if the team is empty
            if (team2.getMembers().isEmpty()) {
                hashComponents.remove(team);
            }
        }
    }
}
