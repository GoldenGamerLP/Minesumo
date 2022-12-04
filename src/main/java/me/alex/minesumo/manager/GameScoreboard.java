package me.alex.minesumo.manager;

import me.alex.minesumo.data.instances.Arena;
import me.hsgamer.hscore.minestom.board.Board;
import net.minestom.server.entity.Player;
import org.apache.logging.log4j.util.TriConsumer;

import java.time.Duration;
import java.util.function.BiConsumer;

public abstract class GameScoreboard{

    private final Arena arena;

    public GameScoreboard(Arena arena) {
        this.arena = arena;
    }

    public void tickBoard(BiConsumer<Player,Arena> update) {
        update.accept();
    };
}
