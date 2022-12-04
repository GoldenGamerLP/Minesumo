package me.alex.minesumo.events;

import me.alex.minesumo.data.instances.Arena;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class ArenaChangeStateEvent implements InstanceEvent {

    private final Arena arena;
    private final Arena.ArenaState state;

    public ArenaChangeStateEvent(Arena arena, Arena.ArenaState before) {
        this.arena = arena;
        this.state = before;
    }

    @Override
    public @NotNull Instance getInstance() {
        return arena;
    }

    public Arena.ArenaState getState() {
        return arena.getState();
    }

    public Arena getArena() {
        return arena;
    }
}
