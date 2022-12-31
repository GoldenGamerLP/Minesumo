package me.alex.minesumo.data.instances;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import me.alex.minesumo.data.Arena;
import me.alex.minesumo.data.ArenaPlayer;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.tasks.*;
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
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public class ArenaImpl extends Arena {

    public static final Duration
            roundStartingTime = Duration.ofSeconds(5),
            roundProcessTime = Duration.ofMinutes(3),
            roundEndingTime = Duration.ofSeconds(5);

    private static final int maxLifes = 0;
    private final MapConfig mapConfig;
    private final Map<UUID, PlayerState> playerStates;
    private final Map<UUID, Integer> playersTeamIds;
    private final AbstractTask
            roundWaitingPlayers = new RoundWaitingTask(this),
            roundStartingTask = new RoundStartingTask(this),
            roundProcessTask = new RoundProcessTask(this),
            roundEndingTask = new RoundEndingTask(this);
    private final Timer timer;
    private final BossBar gameBar;
    private Integer[] lifes;
    private final AtomicReference<ArenaState> state = new AtomicReference<>(ArenaState.LOADING);

    public ArenaImpl(MinesumoInstance instance, MapConfig config) {
        super(UUID.randomUUID(), instance);

        this.mapConfig = config;

        this.timer = new Timer(this.getUniqueId().toString());
        this.gameBar = BossBar.bossBar(Component.empty(), 1F, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

        this.playerStates = new Object2ObjectOpenHashMap<>();
        this.playersTeamIds = new Object2IntArrayMap<>();

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

        if (this.getPlayers().size() == 0) {
            this.unregisterInstance();
        }

        switch (getState()) {
            case NORMAL_STARTING -> {
                this.changeArenaState(ArenaState.WAITING_FOR_PLAYERS);
                //Draw
                //Reset timer
            }
            case INGAME -> {
                int team = this.getTeams().get(player.getUuid());
                if (getPlayers(team).size() == 0) EventDispatcher.call(new TeamEliminatedEvent(this, team, player));
            }
        }
    }

    private void handlePlayerJoinArenaEvent(PlayerJoinArenaEvent playerJoinArenaEvent) {
        Player player = playerJoinArenaEvent.getPlayer();
        //Register
        if (playerJoinArenaEvent.isCancelled()) return;

        player.showBossBar(gameBar);

        switch (getState()) {
            case ENDING, NORMAL_STARTING, INGAME -> //Spectators only see spectators
                    makePlayerSpectator(player);

            case WAITING_FOR_PLAYERS -> {
                //register player
                this.playerStates.put(player.getUuid(), PlayerState.ALIVE);

                //Player can only see each other in a instance
                player.setRespawnPoint(this.mapConfig.getSpectatorPosition());
                player.teleport(this.mapConfig.getSpectatorPosition());
                player.updateViewerRule(entity -> true);
                player.updateViewableRule(player1 -> this.getPlayers().contains(player1));
                player.setGameMode(GameMode.ADVENTURE);

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
        player.updateViewableRule(player1 -> this.playerStates.get(player1.getUuid()).equals(PlayerState.ALIVE));
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(this.mapConfig.getSpectatorPosition());

        //register player
        this.playerStates.put(player.getUuid(), PlayerState.SPECTATOR);
    }


    public void unregisterInstance() {
        getPlayers().forEach(player -> player.kick("Ended Arena"));

        this.timer.cancel();
        this.timer.purge();

        this.playersTeamIds.clear();
        this.playerStates.clear();

        this.scheduler().scheduleTask(() -> {
            log.info("Ended Arena {}", ArenaImpl.this.mapConfig.getMapName());

            MinecraftServer.getInstanceManager().unregisterInstance(ArenaImpl.this);
        }, TaskSchedule.millis(100), TaskSchedule.stop());
    }

    private void handlePlayerOutOfArena(PlayerOutOfArenaEvent event) {
        ArenaPlayer player = (ArenaPlayer) event.getPlayer();
        switch (getState()) {
            case WAITING_FOR_PLAYERS, ENDING -> teleportPlayerToSpawn(player);
            case INGAME -> {
                if (this.playerStates.get(player.getUuid()) == PlayerState.SPECTATOR) {
                    teleportPlayerToSpawn(player);
                    return;
                }

                //Player Death
                Integer pteam = this.playersTeamIds.get(player.getUuid());
                Integer plifes = lifes[pteam];

                EventDispatcher.call(new PlayerDeathEvent(this, player, pteam, plifes));

                if (plifes == 0) {
                    makePlayerSpectator(player);

                    List<Integer> livingTeam = getLivingTeams();

                    if (!livingTeam.contains(pteam))
                        //Team Eliminated
                        EventDispatcher.call(new TeamEliminatedEvent(this, pteam, player));

                    int size = livingTeam.size();
                    if (size <= 1) {
                        ArenaEndEvent.EndState state = size == 0 ? ArenaEndEvent.EndState.DRAW : ArenaEndEvent.EndState.WIN;
                        List<ArenaPlayer> players = size == 0 ? getPlayers(PlayerState.ALIVE) : getPlayers(livingTeam.get(0));
                        int teamID = size == 0 ? 0 : livingTeam.get(0);

                        EventDispatcher.call(new ArenaEndEvent(this, state, players, teamID));
                        this.changeArenaState(ArenaState.ENDING);
                    }
                } else lifes[pteam] = plifes - 1;

                //Todo: Figure out who killed who and add kills
                teleportPlayerToSpawn(player);
            }
        }
    }

    public void changeArenaState(ArenaState state) {
        this.state.set(state);
        EventDispatcher.call(new ArenaChangeStateEvent(this, state));
    }

    public Map<UUID, Integer> getTeams() {
        return playersTeamIds;
    }

    public ArenaState getState() {
        return state.getAcquire();
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

    public void addPlayersToTeam() {
        if (lifes.length != 0) return;

        List<List<ArenaPlayer>> teams = ListUtils.distributeNumbers(
                this.getPlayers(PlayerState.ALIVE),
                this.mapConfig.getGetSpawnPositions().size());

        this.lifes = new Integer[teams.size() + 1];


        for (int team = 0; team < teams.size(); team++) {
            List<ArenaPlayer> players = teams.get(team);

            //Skip empty teams
            if (players.size() == 0) continue;

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
        if (!this.playerStates.containsKey(player.getUuid())
                || !this.playersTeamIds.containsKey(player.getUuid())) {
            //Only specators
            player.teleport(this.mapConfig.getSpectatorPosition());
            return;
        }


        //Ingame players
        int playerTeam = this.playersTeamIds.get(player.getUuid());
        player.teleport(this.mapConfig.getGetSpawnPositions().get(playerTeam));
    }

    public List<Integer> getLivingTeams() {
        return playersTeamIds.entrySet()
                .stream()
                .filter(uuidIntegerEntry -> this.playerStates.get(uuidIntegerEntry.getKey()).equals(PlayerState.ALIVE))
                .map(Map.Entry::getValue)
                .distinct().toList();
    }

    public Map<UUID, PlayerState> getPlayerStates() {
        return playerStates;
    }

    public Map<UUID, Integer> getPlayersTeamIds() {
        return playersTeamIds;
    }

    public BossBar getGameBar() {
        return gameBar;
    }

    public Integer[] getLifes() {
        return lifes;
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

}
