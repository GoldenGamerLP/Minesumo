package me.alex.minesumo.data.instances;

import kotlin.Pair;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Team implements PacketGroupingAudience {

    private final List<Player> players;
    private final TeamState state = TeamState.ALIVE;

    private final Map<java.util.UUID, Pair<Integer, Integer>> kd;

    public Team(List<Player> players) {
        this.players = players;
        this.kd = new HashMap<>();
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.players;
    }

    public Map<java.util.UUID, Pair<Integer, Integer>> getKd() {
        return kd;
    }

    enum TeamState {
        ALIVE,
        DEAD
    }
}
