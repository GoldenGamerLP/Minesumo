package me.alex.minesumo.events;

import me.alex.minesumo.data.instances.Arena;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class PlayerArenaDeathEvent implements InstanceEvent, PlayerEvent {

    private final Player player;
    private final Arena arena;
    private Pos newPlayerPosition = null;

    public PlayerArenaDeathEvent(Player player, Arena arena) {
        this.player = player;
        this.arena = arena;
    }

    @Override
    public @NotNull Instance getInstance() {
        return arena;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    public Pos getNewPlayerPosition() {
        return newPlayerPosition;
    }

    public void setNewPlayerPosition(Pos newPlayerPosition) {
        this.newPlayerPosition = newPlayerPosition;
    }
}
