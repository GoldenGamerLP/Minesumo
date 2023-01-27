package me.alex.minesumo.instances;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.data.entities.MapConfig;
import me.alex.minesumo.events.*;
import me.alex.minesumo.listener.PvPEvents;
import me.alex.minesumo.tasks.*;
import me.alex.minesumo.utils.ListUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ArenaImpl extends AbstractArena {
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
    private final AtomicReference<ArenaState> state = new AtomicReference<>(ArenaState.LOADING);
    private Integer[] lives;

    public ArenaImpl(MinesumoInstance instance, MapConfig config, String gameID) {
        super(UUID.randomUUID(), instance, config, gameID);

        log.info("Initializing Arena {} arenaUID and with UUID {}", this.getGameID(), this.uniqueId);

        this.mapConfig = config;

        this.timer = new Timer(this.getGameID());
        this.gameBar = BossBar.bossBar(
                Component.text(this.getGameID()),
                1F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS);

        this.playerStates = new Object2ObjectOpenHashMap<>();
        this.playersTeamIds = new Object2IntArrayMap<>();

        this.timer.schedule(new ArenaPlayerCheck(this), 0, 250);

        registerListener();

        //Register
        MinecraftServer.getInstanceManager().registerSharedInstance(this);

        //Only let players join if the arena is ready
        changeArenaState(ArenaState.WAITING_FOR_PLAYERS);
    }

    private void registerListener() {
        EventNode<InstanceEvent> node = this.eventNode();

        node.addListener(PlayerOutOfArenaEvent.class, this::handlePlayerOutOfArena);
        node.addListener(EntitySpawnEvent.class, this::handlePlayerJoinArenaEvent);
        node.addListener(RemoveEntityFromInstanceEvent.class, this::handlePlayerLeaveArenaEvent);
        node.addListener(PlayerDisconnectEvent.class, event -> {
            //Unregister instance if 1 or fewer players are in the instance
            if (this.getPlayers().size() <= 1) {
                if (this.getState() == ArenaState.ENDING) return;
                this.unregisterInstance();
            }
        });

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
                        Duration.ofSeconds(1).toMillis()
                );
                case INGAME -> this.timer.scheduleAtFixedRate(
                        this.roundProcessTask,
                        0,
                        Duration.ofSeconds(1).toMillis());
                case ENDING -> this.timer.scheduleAtFixedRate(
                        this.roundEndingTask,
                        0,
                        Duration.ofSeconds(1).toMillis());
                default -> {
                }
            }
        });

    }


    private void handlePlayerLeaveArenaEvent(RemoveEntityFromInstanceEvent event) {
        //Get the player who left the arena
        if (!(event.getEntity() instanceof Player player)) return;

        //Hide the boss bar
        player.hideBossBar(gameBar);

        //If the player was not playing in this arena, ignore the event
        if (!this.playerStates.containsKey(player.getUuid())) return;
        //If player was spectator - ignore
        if (this.playerStates.remove(player.getUuid()) == PlayerState.SPECTATOR) return;

        //If the arena is in the starting state, switch it to waiting for players
        switch (getState()) {
            case NORMAL_STARTING -> this.changeArenaState(ArenaState.WAITING_FOR_PLAYERS);
            case INGAME -> {
                //Get a list of living teams
                List<Integer> teams = getLivingTeams();
                //Get the player's team
                int team = this.getTeams().get(player.getUuid());

                //Call the PlayerDeathEvent
                EventDispatcher.call(new PlayerDeathEvent(this, player, null, team, lives[team]));

                //If the team has no more players, call the TeamEliminatedEvent
                if (!teams.contains(team))
                    EventDispatcher.call(new TeamEliminatedEvent(this, team, player, null));

                //Last team/player standing
                if (teams.size() == 1) {
                    EventDispatcher.call(new ArenaEndEvent(this, ArenaEndEvent.EndState.DRAW, List.of(), 0));
                    this.changeArenaState(ArenaState.ENDING);
                }
            }
            default -> {
            }
        }

        //Call the PlayerLeaveArenaEvent
        EventDispatcher.call(new PlayerLeaveArenaEvent(this, player));
    }

    private void handlePlayerJoinArenaEvent(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        player.showBossBar(gameBar);

        switch (getState()) {
            case ENDING, NORMAL_STARTING, INGAME -> {
                makePlayerSpectator(player);
            }
            case WAITING_FOR_PLAYERS -> {
                this.playerStates.put(player.getUuid(), PlayerState.ALIVE);
                player.setRespawnPoint(this.mapConfig.getSpectatorPosition());
                player.teleport(this.mapConfig.getSpectatorPosition());
                player.setGameMode(GameMode.ADVENTURE);

                if (getPlayerFromState(PlayerState.ALIVE).size() == getMaxPlayers())
                    this.changeArenaState(ArenaState.NORMAL_STARTING);
            }
            default -> throw new IllegalStateException("Player cannot be in a loaded arena!");
        }

        EventDispatcher.call(new PlayerJoinArenaEvent(this, player));
    }

    private void makePlayerSpectator(Player player) {
        //Spectators only see spectators and players cant see spectators use player#setViewable
        player.updateViewableRule(player1 -> player1.getGameMode() == GameMode.SPECTATOR || player1.getGameMode() == GameMode.ADVENTURE);

        //register player
        player.setAutoViewable(false);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(this.mapConfig.getSpectatorPosition());

        this.playerStates.put(player.getUuid(), PlayerState.SPECTATOR);
    }


    /**
     * Unregisters this instance from the game.
     */
    public void unregisterInstance() {
        // Kick all players
        getPlayers().forEach(player -> player.kick("Ended Arena"));

        // Cancel and purge the timer
        this.timer.cancel();
        this.timer.purge();

        // Schedule the code that will run after the timer is cancelled and purged
        this.scheduler().scheduleTask(() -> {
            // Clear the player teams
            this.playersTeamIds.clear();
            this.playerStates.clear();

            // Unregister the instance
            MinecraftServer.getInstanceManager().unregisterInstance(ArenaImpl.this);
            log.info("Ended Arena {} with UUID {}", this.getGameID(), this.uniqueId);
        }, TaskSchedule.millis(100), TaskSchedule.stop());
    }

    private void handlePlayerOutOfArena(PlayerOutOfArenaEvent event) {
        Player player = event.getPlayer();
        switch (getState()) {
            case WAITING_FOR_PLAYERS, ENDING -> teleportPlayerToSpawn(player);
            case INGAME -> {
                if (this.playerStates.get(player.getUuid()) == PlayerState.SPECTATOR) {
                    teleportPlayerToSpawn(player);
                    return;
                }
                //Player Death
                Integer pteam = this.playersTeamIds.get(player.getUuid());
                Player attacker = MinecraftServer.getConnectionManager()
                        .getPlayer(player.getTag(PvPEvents.LAST_HIT));
                EventDispatcher.call(new PlayerDeathEvent(this, player, attacker, pteam, lives[pteam]));
                if (lives[pteam] == 0) {
                    makePlayerSpectator(player);
                    //For PVP
                    player.removeTag(PvPEvents.TEAM_TAG);
                    List<Integer> livingTeam = getLivingTeams();
                    if (!livingTeam.contains(pteam))
                        //Team Eliminated
                        EventDispatcher.call(new TeamEliminatedEvent(this, pteam, player, attacker));
                    int size = livingTeam.size();
                    if (size <= 1) {
                        //End Arena because last team standing
                        ArenaEndEvent.EndState state = size == 0 ? ArenaEndEvent.EndState.DRAW : ArenaEndEvent.EndState.WIN;
                        List<Player> players = size == 0 ? getPlayerFromState(PlayerState.ALIVE) : getPlayersFromTeam(livingTeam.get(0));
                        int teamID = size == 0 ? 0 : livingTeam.get(0);
                        EventDispatcher.call(new ArenaEndEvent(this, state, players, teamID));
                        this.changeArenaState(ArenaState.ENDING);
                    }
                } else lives[pteam]--;
                teleportPlayerToSpawn(player);
            }
            default -> {
            }
        }
    }

    public void changeArenaState(ArenaState state) {
        this.state.set(state);
        EventDispatcher.call(new ArenaChangeStateEvent(this, state));
    }

    public void addPlayersToTeam() {
        // Distribute players evenly among the teams.
        List<List<Player>> teams = ListUtils.distributeNumbers(
                this.getPlayerFromState(PlayerState.ALIVE),
                this.mapConfig.getSpawnPositions().size());

        this.lives = new Integer[teams.size() + 1];

        for (int team = 0; team < teams.size(); team++) {
            List<Player> players = teams.get(team);

            // Skip empty teams
            if (players.size() == 0) continue;

            // Set the lives of this team
            this.lives[team] = this.getMaxLives() * players.size();

            for (Player player : players) {
                // Assign the player to this team
                this.playersTeamIds.put(player.getUuid(), team);

                // FOR PvP
                player.setTag(PvPEvents.TEAM_TAG, team);
            }
        }
    }

    public void teleportPlayerToSpawn(Player player) {
        // Only spectators are teleported to the spectator spawn.
        if (!this.playerStates.containsKey(player.getUuid())
                || !this.playersTeamIds.containsKey(player.getUuid())) {
            player.teleport(this.mapConfig.getSpectatorPosition());
            return;
        }

        // Ingame players are teleported to their team spawn.
        int playerTeam = this.playersTeamIds.get(player.getUuid());
        player.teleport(this.mapConfig.getSpawnPositions().get(playerTeam));
    }

    public List<Player> getPlayerFromState(PlayerState playerState) {
        return this.playerStates.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(playerState))
                .map(Map.Entry::getKey)
                .map(MinecraftServer.getConnectionManager()::getPlayer)
                .toList();
    }

    public int getMaxPlayers() {
        return this.mapConfig.getSpawnPositions().size() * this.mapConfig.getPlayerPerSpawnPosition();
    }

    public List<Player> getPlayersFromTeam(final Integer team) {
        return playersTeamIds.entrySet()
                .stream()
                .filter(uuidIntegerEntry -> Objects.equals(uuidIntegerEntry.getValue(), team))
                .map(Map.Entry::getKey)
                .map(MinecraftServer.getConnectionManager()::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Integer> getLivingTeams() {
        return playersTeamIds.entrySet()
                .stream()
                .filter(uuidIntegerEntry -> this.playerStates.containsKey(uuidIntegerEntry.getKey()))
                .filter(uuidIntegerEntry -> this.playerStates.get(uuidIntegerEntry.getKey()).equals(PlayerState.ALIVE))
                .map(Map.Entry::getValue)
                .distinct().toList();
    }

    public Map<UUID, Integer> getPlayersTeamIds() {
        return playersTeamIds;
    }

    public BossBar getGameBar() {
        return gameBar;
    }

    public Integer[] getLives() {
        return lives;
    }

    public Map<UUID, Integer> getTeams() {
        return playersTeamIds;
    }

    public ArenaState getState() {
        return state.get();
    }

    public MapConfig getMapConfig() {
        return mapConfig;
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
