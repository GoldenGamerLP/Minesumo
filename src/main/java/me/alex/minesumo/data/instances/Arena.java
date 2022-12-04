package me.alex.minesumo.data.instances;

import kotlin.Pair;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.ArenaChangeStateEvent;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.events.PlayerJoinArenaEvent;
import me.alex.minesumo.events.PlayerLeaveArenaEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Arena extends SharedInstance {

    private final Map<UUID,Pair<PlayerState,Player>> teams;
    private final Map<UUID,PlayerState> playerStates;
    private ArenaState state = ArenaState.WAITING_FOR_PLAYERS;
    private final MapConfig mapConfig;
    private final Scheduler scheduler = this.scheduler();
    private static final Duration
            roundStartingTime = Duration.ofSeconds(3),
            roundProcessTime = Duration.ofMinutes(3),
            roundEndingTime = Duration.ofSeconds(3);

    private static final int lifes = 1;

    private Task
            roundStartingTask,
            roundProcessTask,
            roundEndingTask;

    public Arena(MinesumoInstance instance, MapConfig config) {
        super(UUID.randomUUID(), instance);

        this.teams = new HashMap<>();
        this.playerStates = new HashMap<>();
        this.mapConfig = config;

        BoundingBox boundingBox = new BoundingBox(30,30,30);
        //Innit Teams, spawns, scoerboard, timers etc

        registerListener();

        //Register
        MinecraftServer.getInstanceManager().registerSharedInstance(this);
    }

    //Change! Only 1 v 1 update!
    ////////////////////////////////
    ////////////////////////////////

    private void registerListener() {
        EventNode<InstanceEvent> node = this.eventNode();

        node.addListener(PlayerArenaDeathEvent.class, playerDeathEvent -> {

        });

        node.addListener(PlayerJoinArenaEvent.class,playerJoinArenaEvent -> {
            Player player = playerJoinArenaEvent.getPlayer();
            //Register
            player.setAutoViewable(false);
            player.updateViewableRule(player1 -> this.getPlayers().contains(player1));

           switch (this.state) {
               case ENDING -> {
                   playerJoinArenaEvent.setCancelled(true);
               }
               case WAITING_FOR_PLAYERS -> {
                   player.teleport(this.mapConfig.getSpectatorPosition());
                   player.setGameMode(GameMode.ADVENTURE);

                   //Send messages to players
                   if(this.roundStartingTask == null) this.roundStartingTask = scheduler.buildTask(() -> {
                       this.forEachAudience(audience ->
                               audience.sendMessage(Component.text("Waiting for players")));
                   }).repeat(Duration.ofSeconds(3)).schedule();
               }
               case NORMAL_STARTING -> {
                   player.setGameMode(GameMode.SPECTATOR);
               }
           }
        });

        node.addListener(PlayerLeaveArenaEvent.class,playerLeaveArenaEvent -> {
           Player player = playerLeaveArenaEvent.getPlayer();
           switch (this.state) {
               case WAITING_FOR_PLAYERS -> {
                   boolean isLastPlayer = this.getPlayers().isEmpty();

                   if(isLastPlayer) {
                       this.roundStartingTask.cancel();
                       this.roundStartingTask = null;

                       this.unregisterInstance();
                   }
                   //Stop cooldown timer
               }
               case NORMAL_STARTING -> {
                   //Draw
               }
               case ENDING -> {
                   //If every player left, unregister instance
                   if(this.getPlayers().size() == 0) MinecraftServer
                           .getInstanceManager()
                           .unregisterInstance(this);
               }
           }
        });
    }

    public void changeArenaPhase(ArenaState currentState, ArenaState newState) {

    }

    private void unregisterInstance() {
        MinecraftServer.getInstanceManager().unregisterInstance(this);
    }

    private void handlePlayerDeath(PlayerArenaDeathEvent event) {
        Player player = event.getPlayer();
        switch (this.state) {
            case WAITING_FOR_PLAYERS, ENDING -> player.teleport(this.mapConfig.getSpectatorPosition());
            case INGAME -> {

            }
        }
    }

    private void changeArenaState(ArenaState state) {
        ArenaState currentState = this.state;
        ArenaState newState = state;
        EventDispatcher.call(new ArenaChangeStateEvent(this, currentState));

        this.state = newState;
    }

    public enum ArenaState{
        WAITING_FOR_PLAYERS,
        NORMAL_STARTING,
        INGAME,
        ENDING;
    }

    public enum ArenaMode {
        OneVsOne,
        OTHER;
    }

    public enum PlayerState {
        SPECTATOR,
        ALIVE;
    }

    public Map<String, Pair<PlayerState, List<Player>>> getTeams() {
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
}
