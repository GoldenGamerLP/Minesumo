package me.alex.minesumo.events;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class PlayerArenaDeathEvent implements InstanceEvent, PlayerEvent {

    private final Player player;
    private final ArenaImpl arenaImpl;
    private Pos newPlayerPosition = null;

    public PlayerArenaDeathEvent(Player player, ArenaImpl arenaImpl) {
        this.player = player;
        this.arenaImpl = arenaImpl;
    }

    @Override
    public @NotNull Instance getInstance() {
        return arenaImpl;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public ArenaImpl getArena() {
        return arenaImpl;
    }

    public Pos getNewPlayerPosition() {
        return newPlayerPosition;
    }

    public void setNewPlayerPosition(Pos newPlayerPosition) {
        this.newPlayerPosition = newPlayerPosition;
    }
}
