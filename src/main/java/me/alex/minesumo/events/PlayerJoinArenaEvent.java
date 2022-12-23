package me.alex.minesumo.events;

import lombok.Getter;
import me.alex.minesumo.data.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;

@Getter
public class PlayerJoinArenaEvent extends AddEntityToInstanceEvent {

    private final Player player;
    private final ArenaImpl arenaImpl;

    public PlayerJoinArenaEvent(ArenaImpl arenaImpl, Player player) {
        super(arenaImpl, player);

        this.arenaImpl = arenaImpl;
        this.player = player;
    }


}
