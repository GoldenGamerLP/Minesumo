package me.alex.minesumo.events;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class PlayerDeathEvent implements InstanceEvent {

    private final Player player;
    private final int teamId;

    private final int leftLifes;

    private final Instance instance;

    public PlayerDeathEvent(ArenaImpl instance, Player player, int teamId, int leftLifes) {
        this.player = player;
        this.teamId = teamId;
        this.leftLifes = leftLifes;
        this.instance = instance;
    }

    public Player getPlayer() {
        return player;
    }

    public int getTeamId() {
        return teamId;
    }

    public int getLeftLifes() {
        return leftLifes;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }
}
