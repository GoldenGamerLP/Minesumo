package me.alex.minesumo.events;

import lombok.Getter;
import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;

@Getter
public class PlayerLeaveArenaEvent extends RemoveEntityFromInstanceEvent {

    private final Player player;
    private final ArenaImpl arenaImpl;

    public PlayerLeaveArenaEvent(ArenaImpl arenaImpl, Player player) {
        super(arenaImpl, player);

        this.arenaImpl = arenaImpl;
        this.player = player;
    }
}
