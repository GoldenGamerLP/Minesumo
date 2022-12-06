package me.alex.minesumo.events;

import lombok.Getter;
import me.alex.minesumo.data.instances.Arena;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;

@Getter
public class PlayerJoinArenaEvent extends AddEntityToInstanceEvent {

    private final Player player;
    private final Arena arena;

    public PlayerJoinArenaEvent(Arena arena, Player player) {
        super(arena, player);

        this.arena = arena;
        this.player = player;
    }


}
