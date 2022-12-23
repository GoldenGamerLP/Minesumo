package me.alex.minesumo.data.instances;

import me.alex.minesumo.data.Arena;
import me.alex.minesumo.data.ArenaPlayer;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.ArenaChangeStateEvent;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.events.PlayerJoinArenaEvent;
import me.alex.minesumo.events.PlayerLeaveArenaEvent;
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

    private static final int maxLifes = 1;
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
    private ArenaState state = null;

    public ArenaImpl(MinesumoInstance instance, MapConfig config) {
        super(UUID.randomUUID(), instance);

        this.mapConfig = config;

        this.timer = new Timer(this.getUniqueId().toString());
        this.gameBar = BossBar.bossBar(Component.empty(), 1F, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        this.playerStates = new WeakHashMap<>();
        this.playersTeamIds = new WeakHashMap<>();


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
        node.addListener(PlayerJoinArenaEvent.class, this::handlePlayerJoinArenaEvent);
        node.addListener(PlayerLeaveArenaEvent.class, this::handlePlayerLeaveArenaEvent);

        EventListener<PlayerMoveEvent> moveEvent = EventListener.builder(PlayerMoveEvent.class)
                .filter(ignored -> this.state.equals(ArenaState.NORMAL_STARTING))
                .filter(playerMoveEvent -> this.playerStates.get(playerMoveEvent.getPlayer().getUuid()).equals(PlayerState.ALIVE))
                .handler(playerMoveEvent -> playerMoveEvent.setCancelled(true))
                .build();

        node.addListener(moveEvent);
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

    }


    private void handlePlayerLeaveArenaEvent(PlayerLeaveArenaEvent playerLeaveArenaEvent) {
        Player player = playerLeaveArenaEvent.getPlayer();

        if (playerLeaveArenaEvent.getArenaImpl() == null) return;
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
            case ENDING -> {
                //If every player left, unregister instance
                if (this.getPlayers().size() == 0) MinecraftServer
                        .getInstanceManager()
                        .unregisterInstance(this);
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
                player.setAutoViewable(false);
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

    private void handlePlayerDeath(PlayerArenaDeathEvent event) {
        ArenaPlayer player = (ArenaPlayer) event.getPlayer();
        switch (this.state) {
            case WAITING_FOR_PLAYERS, ENDING -> player.teleport(this.mapConfig.getSpectatorPosition());
            case INGAME -> {
                if (this.playerStates.get(player.getUuid()) == PlayerState.SPECTATOR) {
                    event.setNewPlayerPosition(this.mapConfig.getSpectatorPosition());
                    return;
                }

                Integer pteam = this.playersTeamIds.get(player.getUuid());
                Integer plifes = lifes[pteam];


                if (plifes == 0) {
                    //Death - Spectator
                    player.sendMessage("You are now a spectator");
                    return;
                } else lifes[pteam] -= 1;

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

    private void changeArenaState(ArenaState state) {
        ArenaState currentState = this.state;
        EventDispatcher.call(new ArenaChangeStateEvent(this, currentState));

        this.state = state;
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
                else sb.append(players.size() == 1 ? players.get(0) : players.size());

                if (i > lifes.length - 1) sb.append(", ");
            }
            gameBar.name(Component.translatable(sb.toString()));

            seconds--;
            if (seconds % 10 == 0) sendMessage(Component.translatable("ending in"));
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
                getPlayers().forEach(player -> player.kick(""));
                unregisterInstance();
                timer.cancel();
                timer.purge();
                this.cancel();
            }

            sendMessage(Component.translatable("round.ending"));
            seconds--;
        }


    }

}
