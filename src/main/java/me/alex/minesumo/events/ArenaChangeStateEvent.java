package me.alex.minesumo.events;

import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class ArenaChangeStateEvent implements InstanceEvent {

    private final ArenaImpl arenaImpl;

    public ArenaChangeStateEvent(ArenaImpl arenaImpl, ArenaImpl.ArenaState before) {
        this.arenaImpl = arenaImpl;
    }

    @Override
    public @NotNull Instance getInstance() {
        return arenaImpl;
    }

    public ArenaImpl.ArenaState getState() {
        return arenaImpl.getState();
    }

    public ArenaImpl getArena() {
        return arenaImpl;
    }
}
