package me.alex.minesumo.data.tasks;

import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import static me.alex.minesumo.instances.AbstractArena.roundEndingTime;

public class RoundEndingTask extends AbstractTask {
    private long seconds;

    public RoundEndingTask(ArenaImpl arena) {
        super(arena);

        this.seconds = roundEndingTime.getSeconds();
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.ENDING) {
            this.cancel();
            return;
        }
        if (seconds == 0)
            arena.unregisterInstance();

        Component translated = Messages.GAME_ENDING.toTranslatable(Component.text(seconds));

        if (seconds % 10 == 0 || seconds <= 3)
            arena.sendMessage(translated);

        BossBar bar = arena.getGameBar();
        bar.progress(seconds * 1F / roundEndingTime.getSeconds());
        bar.name(translated);
        bar.color(BossBar.Color.RED);

        seconds--;
    }
}
