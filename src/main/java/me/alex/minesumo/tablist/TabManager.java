package me.alex.minesumo.tablist;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.scoreboard.TeamBuilder;
import net.minestom.server.scoreboard.TeamManager;
import net.minestom.server.thread.Acquirable;
import org.jetbrains.annotations.Blocking;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    private static final Map<Integer, Team> hashComponents = new HashMap<>();
    private static final Map<UUID, Integer> playerTeams = new ConcurrentHashMap<>(6);
    private static final EventNode<PlayerEvent> NODE = EventNode.type("minesumo:tablist", EventFilter.PLAYER);
    private static BiConsumer<Player, TeamTemplate.TeamBuilder> defPrefix = (player, teamBuilder) -> teamBuilder.withColor(NamedTextColor.GRAY);

    static {
        //Prefix on player spawn event
        NODE.addListener(AsyncPlayerPreLoginEvent.class, event -> {
            Acquirable<Player> player = event.getPlayer().getAcquirable();
            player.sync(TabManager::resetPlayer);
        });

        NODE.addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            removePlayer(player);
        });

        NODE.addListener(TablistPlayerChangeEvent.class, event -> {
            Player player = event.getPlayer();
            TeamTemplate.TeamBuilder builder = TeamTemplate.TeamBuilder.newBuilder();
            event.getTeamBuilder().accept(builder);
            TeamTemplate team = builder.build();

            int hash = getHash(team.getPrefix(), team.getSuffix());

            //Is in the same team
            if (playerTeams.containsKey(player.getUuid()) && playerTeams.get(player.getUuid()) == hash)
                return;


            //Register the team
            registerTeam(hash + "", team.getPriority(), teamBuilder -> {
                teamBuilder.prefix(team.getPrefix());
                teamBuilder.suffix(team.getSuffix());
                teamBuilder.teamColor(team.getColor());
                teamBuilder.collisionRule(TeamsPacket.CollisionRule.NEVER);
                teamBuilder.updateNameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS);
                teamBuilder.seeInvisiblePlayers();
            });

            //Update the player
            updatePlayer(player, hash);

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

    //Reset

    /**
     * Resets the player to the default prefix
     *
     * @param player the player
     */
    @Blocking
    public static void resetPlayer(Player player) {
        TeamTemplate.TeamBuilder builder = TeamTemplate.TeamBuilder.newBuilder();
        defPrefix.accept(player, builder);
        TeamTemplate team = builder.build();

        TablistPlayerChangeEvent tabChangeEvent = new TablistPlayerChangeEvent(player, teamBuilder -> {
            teamBuilder.withPrefix(team.getPrefix());
            teamBuilder.withSuffix(team.getSuffix());
            teamBuilder.withColor(team.getColor());
        });

        EventDispatcher.call(tabChangeEvent);
    }

    /**
     * Registers a team if it is not already registered
     *
     * @param team        the team name
     * @param teamBuilder the team builder
     */
    private static void registerTeam(String team, int priority, Consumer<TeamBuilder> teamBuilder) {
        String teamName = priority + "_" + team;
        if (teamManager.getTeam(teamName) == null) {
            TeamBuilder builder = teamManager.createBuilder(teamName);
            teamBuilder.accept(builder);
            //Update the hashComponents
            hashComponents.put(getHash(builder.build()), teamManager.getTeam(teamName));
        }
    }

    /**
     * Sets the default prefix for every individual player
     *
     * @param prefix the prefix
     */
    public static void defaultPrefix(BiConsumer<Player, TeamTemplate.TeamBuilder> prefix) {
        defPrefix = prefix;
    }

    /**
     * Updates the player with the team name
     *
     * @param player the player
     * @param hash   the has registered team
     */
    private static void updatePlayer(Player player, Integer hash) {
        Team team1 = hashComponents.get(hash);
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
                teamManager.deleteTeam(firstTeam);
            }
        }

        team1.addMember(player.getUsername());
        //Update the players team with the hashcode
        playerTeams.put(player.getUuid(), getHash(team1));
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
                teamManager.deleteTeam(team2);
            }
        }
    }

    private static int getHash(Component prefix, Component suffix) {
        return Objects.hash(prefix, suffix);
    }

    private static int getHash(Team team) {
        return getHash(team.getPrefix(), team.getSuffix());
    }
}
