package me.alex.minesumo.tasks;

import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

public class RoundWaitingTask extends AbstractTask {

    public RoundWaitingTask(ArenaImpl arena) {
        super(arena);
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.WAITING_FOR_PLAYERS) {
            this.cancel();
            return;
        }

        if (arena.getMaxPlayers() == arena.getPlayerFromState(ArenaImpl.PlayerState.ALIVE).size())
            arena.changeArenaState(ArenaImpl.ArenaState.NORMAL_STARTING);


        List<Player> player = arena.getPlayerFromState(ArenaImpl.PlayerState.ALIVE);
        int maxPlayers = arena.getMaxPlayers();

        float percentage = player.size() * 1F / maxPlayers;
        int needed = maxPlayers - player.size();

        BossBar bar = arena.getGameBar();
        bar.progress(percentage);
        bar.name(Messages.GAME_WAITING_PLAYERS.toTranslatable(Component.text(needed)));
        bar.color(BossBar.Color.YELLOW);

        arena.sendActionBar(Messages.GAME_WAITING.toTranslatable());
    }
}
