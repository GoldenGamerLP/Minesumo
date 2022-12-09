package me.alex.minesumo.data.instances;

import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.ArenaChangeStateEvent;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.events.PlayerJoinArenaEvent;
import me.alex.minesumo.events.PlayerLeaveArenaEvent;
import me.alex.minesumo.utils.ListUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.SharedInstance;

import java.time.Duration;
import java.util.*;

public class Arena extends SharedInstance {

    private static final Duration
            roundStartingTime = Duration.ofSeconds(3),
            roundProcessTime = Duration.ofMinutes(3),
            roundEndingTime = Duration.ofSeconds(3);

    private static final int lifes = 1;
    private final Map<Integer, Team> teams;
    private final Map<UUID, PlayerState> playerStates;
    private final Map<UUID, Integer> playersTeamIds;
    private final MapConfig mapConfig;
    private final TimerTask
            roundWaitingPlayers,
            roundStartingTask,
            roundProcessTask,
            roundEndingTask;
    private final Timer timer;
    private ArenaState state = null;

    public Arena(MinesumoInstance instance, MapConfig config) {
        super(UUID.randomUUID(), instance);

        this.teams = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.playersTeamIds = new HashMap<>();
        this.timer = new Timer(this.getUniqueId().toString());
        this.mapConfig = config;

        this.roundWaitingPlayers = new RoundWaitingTask();
        this.roundStartingTask = new RoundStartingTask();
        this.roundProcessTask = new RoundProcessTask();
        this.roundEndingTask = new RoundEndingTask();


        //Innit Teams, spawns, scoerboard, timers etc

        registerListener();

        //Register
        MinecraftServer.getInstanceManager().registerSharedInstance(this);

        //Only let players join if the arena is ready
        changeArenaState(ArenaState.WAITING_FOR_PLAYERS);
    }

    private void registerListener() {
        EventNode<InstanceEvent> node = this.eventNode();

        node.addListener(PlayerArenaDeathEvent.class, this::handlePlayerDeath);

        node.addListener(PlayerJoinArenaEvent.class, playerJoinArenaEvent -> {
            Player player = playerJoinArenaEvent.getPlayer();
            //Register
            player.setAutoViewable(false);

            switch (this.state) {
                case ENDING, NORMAL_STARTING, INGAME -> {
                    //Spectators only see spectators
                    player.updateViewableRule(player1 -> this.getPlayers().contains(player1));
                    player.setGameMode(GameMode.SPECTATOR);

                    //register player
                    this.playerStates.put(player.getUuid(), PlayerState.SPECTATOR);
                }

                case WAITING_FOR_PLAYERS -> {
                    //Player can only see each other in a instance
                    player.updateViewableRule(player1 -> this.getPlayers().contains(player1));
                    player.teleport(this.mapConfig.getSpectatorPosition());
                    player.setGameMode(GameMode.ADVENTURE);

                    //register player
                    this.playerStates.put(player.getUuid(), PlayerState.ALIVE);

                    //Method --> areEnoughPlayers();
                    if (this.getPlayers().size() == getMaxPlayers()) {
                        this.changeArenaState(ArenaState.NORMAL_STARTING);
                    }
                }
            }
        });

        node.addListener(PlayerLeaveArenaEvent.class, playerLeaveArenaEvent -> {
            Player player = playerLeaveArenaEvent.getPlayer();

            //If player was spectator - ignore
            if (this.playerStates.remove(player.getUuid()).equals(PlayerState.SPECTATOR)) {
                return;
            }

            switch (this.state) {
                case WAITING_FOR_PLAYERS -> {
                    //Do nothing?
                }
                case NORMAL_STARTING -> {
                    this.changeArenaState(ArenaState.ENDING);
                    //Draw
                    //Reset timer
                }
                case ENDING -> {
                    //If every player left, unregister instance
                    if (this.getPlayers().size() == 0) MinecraftServer
                            .getInstanceManager()
                            .unregisterInstance(this);
                }
            }
        });

        node.addListener(ArenaChangeStateEvent.class, playerState -> {
            //Start tasks
            switch (this.state) {
                case WAITING_FOR_PLAYERS -> this.timer.scheduleAtFixedRate(
                        this.roundWaitingPlayers,
                        0,
                        Duration.ofSeconds(1).toMillis());
                case NORMAL_STARTING -> this.timer.scheduleAtFixedRate(
                        this.roundStartingTask,
                        0,
                        Duration.ofSeconds(1).toMillis());
                case INGAME -> this.timer.scheduleAtFixedRate(
                        this.roundProcessTask,
                        0,
                        Duration.ofSeconds(1).toMillis());
                case ENDING -> this.timer.scheduleAtFixedRate(
                        this.roundEndingTask,
                        0,
                        Duration.ofSeconds(1).toMillis());
            }
        });

        node.addListener(PlayerMoveEvent.class, playerMoveEvent -> {
            switch (this.state) {
                case NORMAL_STARTING -> {
                    playerMoveEvent.setCancelled(true);
                }
            }
        });
    }

    private void unregisterInstance() {
        MinecraftServer.getInstanceManager().unregisterInstance(this);
    }

    private void handlePlayerDeath(PlayerArenaDeathEvent event) {
        Player player = event.getPlayer();
        switch (this.state) {
            case WAITING_FOR_PLAYERS, ENDING -> player.teleport(this.mapConfig.getSpectatorPosition());
            case INGAME -> {
                if (this.playerStates.get(player.getUuid()) == PlayerState.SPECTATOR) {
                    event.setNewPlayerPosition(this.mapConfig.getSpectatorPosition());
                    return;
                }

                //Remove one life from the players
                //Add a death to the team
                //Add a point for the other players team
            }
        }
    }

    private void changeArenaState(ArenaState state) {
        ArenaState currentState = this.state;
        EventDispatcher.call(new ArenaChangeStateEvent(this, currentState));

        this.state = state;
    }

    public Map<Integer, Team> getTeams() {
        return teams;
    }

    public ArenaState getState() {
        return state;
    }

    public MapConfig getMapConfig() {
        return mapConfig;
    }

    public int getMaxPlayers() {
        return this.mapConfig.getGetSpawnPositions().length * this.mapConfig.getPlayerPerSpawnPosition();
    }

    public List<Player> getPlayers(PlayerState playerState) {
        return this.getPlayers()
                .stream()
                .filter(player -> this.playerStates.get(player.getUuid()).equals(playerState))
                .toList();
    }

    private void addPlayersToTeam() {
        if (this.state != ArenaState.NORMAL_STARTING) return;

        List<List<Player>> teams = ListUtils.distributeNumbers(
                this.getPlayers(PlayerState.ALIVE),
                this.mapConfig.getGetSpawnPositions().length);

        for (int team = 0; team < teams.size(); team++) {
            List<Player> players = teams.get(team);

            this.teams.put(team, new Team(players));

            for (Player player : players) {
                this.playersTeamIds.put(player.getUuid(), team);
            }
        }
    }

    public void teleportPlayerToSpawn(Player player) {
        if (!this.playerStates.containsKey(player.getUuid())) return;
        if (!this.playersTeamIds.containsKey(player.getUuid())) {
            //Only specators
            player.teleport(this.mapConfig.getSpectatorPosition());
        }

        //Ingame players
        int playerTeam = this.playersTeamIds.get(player.getUuid());
        player.teleport(this.mapConfig.getGetSpawnPositions()[playerTeam]);
    }

    public enum ArenaState {
        WAITING_FOR_PLAYERS,
        NORMAL_STARTING,
        INGAME,
        ENDING
    }

    public enum ArenaMode {
        OneVsOne,
        OTHER
    }

    public enum PlayerState {
        SPECTATOR,
        ALIVE
    }

    private class RoundProcessTask extends TimerTask {
        long seconds = roundProcessTime.toSeconds();

        @Override
        public void run() {
            if (state != ArenaState.INGAME) this.cancel();
            if (seconds == 0) {
                changeArenaState(ArenaState.ENDING);
                this.cancel();
            }
            //Update scoreboards

            seconds--;
            if (seconds % 10 == 0)
                forEachAudience(audience -> audience.sendMessage(Component.translatable("ending in")));
        }
    }

    private class RoundStartingTask extends TimerTask {
        long seconds = roundStartingTime.toSeconds();

        @Override
        public void run() {
            if (state != ArenaState.NORMAL_STARTING) this.cancel();
            if (seconds == 0) {
                addPlayersToTeam();
                getPlayers().forEach(Arena.this::teleportPlayerToSpawn);

                changeArenaState(ArenaState.INGAME);
                this.cancel();
            }

            seconds--;
            forEachAudience(audience -> audience.sendMessage(Component.translatable("starting.in")));
        }
    }

    private class RoundWaitingTask extends TimerTask {
        @Override
        public void run() {
            forEachAudience(audience ->
                    audience.sendActionBar(Component.translatable("waiting.players")));
            if (state != ArenaState.WAITING_FOR_PLAYERS) this.cancel();
        }
    }

    private class RoundEndingTask extends TimerTask {
        long seconds = roundEndingTime.getSeconds();

        @Override
        public void run() {
            if (state != ArenaState.ENDING) this.cancel();
            if (seconds == 0) {
                unregisterInstance();
                this.cancel();
            }
            seconds--;
            forEachAudience(audience -> audience.sendMessage(Component.translatable("round.ending")));
        }
    }
}
