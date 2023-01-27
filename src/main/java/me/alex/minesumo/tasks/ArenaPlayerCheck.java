package me.alex.minesumo.tasks;

import me.alex.minesumo.events.PlayerOutOfArenaEvent;
import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;

public class ArenaPlayerCheck extends AbstractTask {

    private final double maxY;

    public ArenaPlayerCheck(ArenaImpl arena) {
        super(arena);

        this.maxY = arena.getMapConfig().getDeathLevel();
    }

    @Override
    void onRun(ArenaImpl arena) {
        arena.getPlayers().forEach(this::check);
    }

    private void check(Player player) {
        double y = player.getPosition().y();
        if (y > maxY) return;

        EventDispatcher.call(new PlayerOutOfArenaEvent(ArenaPlayerCheck.this.arena, player));
    }
}
