package me.alex.minesumo.data.tasks;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.kyori.adventure.text.Component;

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

        if (arena.getMaxPlayers() == arena.getPlayers(ArenaImpl.PlayerState.ALIVE).size())
            arena.changeArenaState(ArenaImpl.ArenaState.NORMAL_STARTING);

        arena.sendMessage(Component.translatable("waiting.players"));
    }
}
