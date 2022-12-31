package me.alex.minesumo.data.instances;

import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Team implements PacketGroupingAudience {

    private final HashMap<UUID, Integer> lifesMap;
    private final int id;
    private final Boolean shareSameHealth;
    private final TeamState state = TeamState.ALIVE;
    private volatile Integer health;

    public Team(Boolean shareSameHealth, Integer id, HashMap<Player, Integer> players) {
        this.id = id;
        this.lifesMap = new HashMap<>();
        this.shareSameHealth = shareSameHealth;

        players.forEach((player, integer) -> {
            lifesMap.put(player.getUuid(), integer);
            health += integer;
        });
    }

    public void addDeath(UUID death) {
        this.lifesMap.computeIfPresent(death, (uuid, integer) -> integer - 1);
    }

    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return this.getOnlinePlayers();
    }


    public List<Player> getOnlinePlayers() {
        return lifesMap.keySet()
                .stream()
                .map(MinecraftServer.getConnectionManager()::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    enum TeamState {
        ALIVE,
        DEAD
    }
}
