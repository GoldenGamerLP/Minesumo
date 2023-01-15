package me.alex.minesumo.tasks;

import me.alex.minesumo.events.PlayerOutOfArenaEvent;
import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;

public class ArenaPlayerCheck extends AbstractTask {

    private final double minY;

    public ArenaPlayerCheck(ArenaImpl arena) {
        super(arena);

        this.minY = arena.getMapConfig().getDeathLevel();
    }

    @Override
    void onRun(ArenaImpl arena) {
        arena.getPlayers().forEach(this::check);
    }

    private void check(Player player) {
        double y = player.getPosition().y();
        if (y > minY) return;

        //Event
        EventDispatcher.call(new PlayerOutOfArenaEvent(ArenaPlayerCheck.this.arena, player));
    }
}
