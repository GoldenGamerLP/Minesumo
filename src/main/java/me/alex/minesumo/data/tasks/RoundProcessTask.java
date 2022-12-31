package me.alex.minesumo.data.tasks;

import me.alex.minesumo.data.ArenaPlayer;
import me.alex.minesumo.data.instances.ArenaImpl;
import me.alex.minesumo.events.ArenaEndEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventDispatcher;

import java.util.List;

import static me.alex.minesumo.data.instances.ArenaImpl.roundProcessTime;

public class RoundProcessTask extends AbstractTask {

    private long seconds;

    public RoundProcessTask(ArenaImpl arena) {
        super(arena);

        this.seconds = roundProcessTime.toSeconds();
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.INGAME) {
            this.cancel();
            return;
        }
        if (seconds == 0) {
            EventDispatcher.call(new ArenaEndEvent(arena, ArenaEndEvent.EndState.DRAW, arena.getPlayers(ArenaImpl.PlayerState.ALIVE), 0));
            arena.changeArenaState(ArenaImpl.ArenaState.ENDING);
            this.cancel();
        }

        //Update scoreboards
        StringBuilder sb = new StringBuilder();
        Integer[] lifes = arena.getLifes();

        for (Integer livingTeam : arena.getLivingTeams()) {
            List<ArenaPlayer> players = arena.getPlayers(livingTeam);
            sb.append("Team: ");
            sb.append(livingTeam);
            sb.append(" - Lifes: ");
            sb.append(lifes[livingTeam]);
            sb.append(" - Players:");
            if (players.size() == 0) sb.append("None");
            else sb.append(players.size() == 1 ? players.get(0).getUsername() : players.size());

            if (livingTeam > lifes.length - 1) sb.append(" | ");
        }

        BossBar bar = arena.getGameBar();
        bar.name(Component.translatable(sb.toString()));
        bar.progress((seconds * 1000F) / roundProcessTime.toMillis());

        if (seconds % 10 == 0 || seconds <= 5)
            arena.sendMessage(Component.translatable("ending.in" + seconds));

        seconds--;
    }
}
