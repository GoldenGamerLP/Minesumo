package me.alex.minesumo.data.tasks;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import static me.alex.minesumo.data.instances.ArenaImpl.roundEndingTime;

public class RoundEndingTask extends AbstractTask {
    private long seconds;

    public RoundEndingTask(ArenaImpl arena) {
        super(arena);

        this.seconds = roundEndingTime.getSeconds();
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.ENDING) this.cancel();
        if (seconds == 0) {
            arena.unregisterInstance();
            this.cancel();
        }

        arena.sendMessage(Component.translatable("round.ending"));

        BossBar bar = arena.getGameBar();
        bar.progress(seconds * 1F / roundEndingTime.getSeconds());
        bar.name(Component.translatable("game.ending" + seconds));

        seconds--;
    }
}
