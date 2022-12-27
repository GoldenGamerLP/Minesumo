package me.alex.minesumo.data.instances;

import me.alex.minesumo.data.Arena;
import me.alex.minesumo.data.ArenaPlayer;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.*;
import me.alex.minesumo.utils.ListUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;

import java.time.Duration;
import java.util.*;

public class ArenaImpl extends Arena {

    private static final Duration
            roundStartingTime = Duration.ofSeconds(3),
            roundProcessTime = Duration.ofMinutes(3),
            roundEndingTime = Duration.ofSeconds(3);

    private static final int maxLifes = 0;
    private final Map<UUID, PlayerState> playerStates;
    private final Map<UUID, Integer> playersTeamIds;
    private final MapConfig mapConfig;
    private final TimerTask
            roundWaitingPlayers,
            roundStartingTask,
            roundProcessTask,
            roundEndingTask;
    private final Timer timer;
    private final BossBar gameBar;
    private Integer[] lifes;
    private ArenaState state = ArenaState.WAITING_FOR_PLAYERS;

    public ArenaImpl(MinesumoInstance instance, MapConfig config) {
        super(UUID.randomUUID(), instance);

        this.mapConfig = config;

        this.timer = new Timer(this.getUniqueId().toString());
        this.gameBar = BossBar.bossBar(Component.empty(), 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

        this.playerStates = new HashMap<>();
        this.playersTeamIds = new HashMap<>();


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

        node.addListener(PlayerOutOfArenaEvent.class, this::handlePlayerOutOfArena);
        node.addListener(PlayerJoinArenaEvent.class, this::handlePlayerJoinArenaEvent);
        node.addListener(PlayerLeaveArenaEvent.class, this::handlePlayerLeaveArenaEvent);
        node.addListener(TeamEliminatedEvent.class, teamEliminatedEvent -> {
            if (this.playersTeamIds.values().stream().distinct().count() == 1) {
                int teamID = (int) this.playersTeamIds.values().toArray()[0];
                EventDispatcher.call(new ArenaWinEvent(getPlayers(teamID), teamID));
            }
        });

        EventListener<PlayerMoveEvent> moveEvent = EventListener.builder(PlayerMoveEvent.class)
                .filter(ignored -> this.state.equals(ArenaState.NORMAL_STARTING))
                .filter(playerMoveEvent -> this.playerStates.get(playerMoveEvent.getPlayer().getUuid()).equals(PlayerState.ALIVE))
                .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true))
                .build();

        node.addListener(moveEvent);
        node.addListener(ArenaChangeStateEvent.class, arena -> {
            //Start tasks
            switch (arena.getArena().getState()) {
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

    }


    private void handlePlayerLeaveArenaEvent(PlayerLeaveArenaEvent playerLeaveArenaEvent) {
        Player player = playerLeaveArenaEvent.getPlayer();

        if (playerLeaveArenaEvent.getArenaImpl() == null) {
            return;
        }
        player.hideBossBar(gameBar);

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
            case INGAME -> {
                int team = this.getTeams().remove(player.getUuid());
                if (getPlayers(team).size() == 0) EventDispatcher.call(new TeamEliminatedEvent(this, team, player));
            }
            case ENDING -> {
                //If every player left, unregister instance

            }
        }
    }

    private void handlePlayerJoinArenaEvent(PlayerJoinArenaEvent playerJoinArenaEvent) {
        Player player = playerJoinArenaEvent.getPlayer();
        //Register
        if (playerJoinArenaEvent.isCancelled()) return;

        player.showBossBar(gameBar);

        switch (this.state) {
            case ENDING, NORMAL_STARTING, INGAME -> {
                //Spectators only see spectators
                makePlayerSpectator(player);
            }

            case WAITING_FOR_PLAYERS -> {
                //Player can only see each other in a instance
                player.setRespawnPoint(this.mapConfig.getSpectatorPosition());
                player.teleport(this.mapConfig.getSpectatorPosition());
                player.setAutoViewable(false);
                player.updateViewableRule(player1 -> this.getPlayers().contains(player1));
                player.setGameMode(GameMode.CREATIVE);

                //register player
                this.playerStates.put(player.getUuid(), PlayerState.ALIVE);

                //Method --> areEnoughPlayers();
                if (this.getPlayers().size() == getMaxPlayers()) {
                    this.changeArenaState(ArenaState.NORMAL_STARTING);
                }
            }
            case LOADING -> playerJoinArenaEvent.setCancelled(true);
        }
    }

    private void makePlayerSpectator(Player player) {
        player.setAutoViewable(false);
        player.updateViewableRule(player1 -> this.getPlayers().contains(player1));
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(this.mapConfig.getSpectatorPosition());

        //register player
        this.playerStates.put(player.getUuid(), PlayerState.SPECTATOR);
    }


    private void unregisterInstance() {
        MinecraftServer.getInstanceManager().unregisterInstance(this);
    }

    private void handlePlayerOutOfArena(PlayerOutOfArenaEvent event) {
        ArenaPlayer player = (ArenaPlayer) event.getPlayer();
        switch (this.state) {
            case WAITING_FOR_PLAYERS, ENDING -> player.teleport(this.mapConfig.getSpectatorPosition());
            case INGAME -> {
                if (this.playerStates.get(player.getUuid()) == PlayerState.SPECTATOR) {
                    event.setNewPlayerPosition(this.mapConfig.getSpectatorPosition());
                    return;
                }

                //Player Death
                Integer pteam = this.playersTeamIds.get(player.getUuid());
                Integer plifes = lifes[pteam];

                EventDispatcher.call(new PlayerDeathEvent(player, pteam, plifes));

                if (plifes == 0) {
                    //Death - Spectator
                    this.playersTeamIds.remove(player.getUuid());

                    if (!this.playersTeamIds.containsValue(pteam)) {
                        //Team Eliminated
                        EventDispatcher.call(new TeamEliminatedEvent(this, pteam, player));
                    }
                    player.sendMessage("You are now a spectator");

                    //Spectator
                    makePlayerSpectator(player);
                    return;
                } else lifes[pteam] = plifes - 1;

                //Todo: Figure out who killed who and add kills

                player.teleport(this.mapConfig.getGetSpawnPositions().get(pteam));
                player.addDeath();
                //Remove one life from the players
                //Add a death to the team
                //Add a point for the other players team

                //Now remove a life

            }
        }
    }

    public void changeArenaState(ArenaState state) {
        this.state = state;
        EventDispatcher.call(new ArenaChangeStateEvent(this, state));
    }

    public Map<UUID, Integer> getTeams() {
        return playersTeamIds;
    }

    public ArenaState getState() {
        return state;
    }

    public MapConfig getMapConfig() {
        return mapConfig;
    }

    public int getMaxPlayers() {
        return this.mapConfig.getGetSpawnPositions().size() * this.mapConfig.getPlayerPerSpawnPosition();
    }

    public List<ArenaPlayer> getPlayers(PlayerState playerState) {
        return this.getPlayers()
                .stream()
                .filter(player -> this.playerStates.get(player.getUuid()).equals(playerState))
                .map(player -> (ArenaPlayer) player)
                .toList();
    }

    private void addPlayersToTeam() {
        if (this.state != ArenaState.NORMAL_STARTING) return;

        List<List<ArenaPlayer>> teams = ListUtils.distributeNumbers(
                this.getPlayers(PlayerState.ALIVE),
                this.mapConfig.getGetSpawnPositions().size());

        this.lifes = new Integer[teams.size() + 1];


        for (int team = 0; team < teams.size(); team++) {
            List<ArenaPlayer> players = teams.get(team);

            //Leben in einem Team
            this.lifes[team] = maxLifes * players.size();

            for (Player player : players) {
                this.playersTeamIds.put(player.getUuid(), team);
            }
        }
    }

    public List<ArenaPlayer> getPlayers(final Integer team) {
        if (!this.playersTeamIds.containsValue(team)) return List.of();
        return playersTeamIds.entrySet()
                .stream()
                .filter(uuidIntegerEntry -> Objects.equals(uuidIntegerEntry.getValue(), team))
                .map(Map.Entry::getKey)
                .map(MinecraftServer.getConnectionManager()::getPlayer)
                .map(player -> (ArenaPlayer) player)
                .toList();
    }

    public void teleportPlayerToSpawn(Player player) {
        if (!this.playerStates.containsKey(player.getUuid())) return;
        if (!this.playersTeamIds.containsKey(player.getUuid())) {
            //Only specators
            player.teleport(this.mapConfig.getSpectatorPosition());
        }


        //Ingame players
        int playerTeam = this.playersTeamIds.get(player.getUuid());
        player.teleport(this.mapConfig.getGetSpawnPositions().get(playerTeam));
    }


    public enum ArenaState {
        LOADING,
        WAITING_FOR_PLAYERS,
        NORMAL_STARTING,
        INGAME,
        ENDING
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
            gameBar.progress((seconds * 1000F) / roundProcessTime.toMillis());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lifes.length; i++) {
                List<ArenaPlayer> players = getPlayers(i);
                sb.append("Team >");
                sb.append(i);
                sb.append(" Lifes >");
                sb.append(lifes[i]);
                sb.append(" Players >");
                if (players.size() == 0) sb.append("None");
                else sb.append(players.size() == 1 ? players.get(0).getUsername() : players.size());

                if (i > lifes.length - 1) sb.append(", ");
            }
            gameBar.name(Component.translatable(sb.toString()));

            seconds--;
            if (seconds % 10 == 0) sendMessage(Component.translatable("ending in" + seconds));
        }

    }

    private class RoundStartingTask extends TimerTask {

        long seconds = roundStartingTime.toSeconds();

        @Override
        public void run() {
            if (state != ArenaState.NORMAL_STARTING) this.cancel();
            if (seconds == 0) {
                addPlayersToTeam();
                getPlayers().forEach(ArenaImpl.this::teleportPlayerToSpawn);
                getPlayers(PlayerState.ALIVE).forEach(player -> player.addHistory(ArenaImpl.this.stats));

                changeArenaState(ArenaState.INGAME);
                this.cancel();
            }

            gameBar.name(Component.translatable("starting.in" + seconds));
            sendMessage(Component.translatable("starting.in"));

            seconds--;
        }

    }

    private class RoundWaitingTask extends TimerTask {

        @Override
        public void run() {
            sendMessage(Component.translatable("waiting.players"));
            if (state != ArenaState.WAITING_FOR_PLAYERS) this.cancel();
        }
    }

    private class RoundEndingTask extends TimerTask {

        long seconds = roundEndingTime.getSeconds();

        @Override
        public void run() {
            if (state != ArenaState.ENDING) this.cancel();
            if (seconds == 0) {
                List.copyOf(getPlayers()).forEach(player -> player.kick("Ended!"));
                timer.cancel();
                timer.purge();
                unregisterInstance();
                this.cancel();
            }

            sendMessage(Component.translatable("round.ending"));
            gameBar.progress(seconds * 1F / roundEndingTime.getSeconds());
            gameBar.name(Component.translatable("game.ending" + seconds));
            seconds--;
        }


    }

}
