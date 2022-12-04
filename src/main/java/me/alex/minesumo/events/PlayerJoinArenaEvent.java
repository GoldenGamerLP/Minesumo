package me.alex.minesumo.events;

import lombok.Getter;
import me.alex.minesumo.data.instances.Arena;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.apache.logging.log4j.core.util.Cancellable;
import org.jetbrains.annotations.NotNull;

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
