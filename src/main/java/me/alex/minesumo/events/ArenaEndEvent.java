package me.alex.minesumo.events;

import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaEndEvent implements InstanceEvent {

    private final List<Player> winningPlayers;
    private final int teamId;
    private final Instance instance;

    private final EndState state;

    public ArenaEndEvent(ArenaImpl instance, EndState endState, List<Player> winningPlayers, int teamId) {
        this.winningPlayers = winningPlayers;
        this.teamId = teamId;
        this.instance = instance;
        this.state = endState;
    }

    public List<Player> getWinningPlayers() {
        return winningPlayers;
    }

    public int getTeamId() {
        return teamId;
    }

    @Override
    public @NotNull Instance getInstance() {
        return this.instance;
    }

    public EndState getState() {
        return state;
    }

    public enum EndState {
        DRAW,
        WIN
    }
}
