package me.alex.minesumo.events;

import me.alex.minesumo.data.ArenaPlayer;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaWinEvent implements InstanceEvent {

    private final List<ArenaPlayer> winningPlayers;
    private final int teamId;

    public ArenaWinEvent(List<ArenaPlayer> winningPlayers, int teamId) {
        this.winningPlayers = winningPlayers;
        this.teamId = teamId;
    }

    public List<ArenaPlayer> getWinningPlayers() {
        return winningPlayers;
    }

    public int getTeamId() {
        return teamId;
    }

    @Override
    public @NotNull Instance getInstance() {
        return null;
    }
}
