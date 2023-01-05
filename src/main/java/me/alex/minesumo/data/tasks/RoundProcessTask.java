package me.alex.minesumo.data.tasks;

import me.alex.minesumo.events.ArenaEndEvent;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;

import java.util.List;

import static me.alex.minesumo.instances.AbstractArena.roundProcessTime;

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
        Integer[] lifes = arena.getLives();

        for (Integer livingTeam : arena.getLivingTeams()) {
            List<Player> players = arena.getPlayers(livingTeam);
            sb.append("Team: ");
            sb.append(livingTeam);
            sb.append(" - Lives: ");
            sb.append(lifes[livingTeam]);
            sb.append(" - Players:");
            if (players.size() == 0) sb.append("None");
            else sb.append(players.size() == 1 ? players.get(0).getUsername() : players.size());

            if (livingTeam > lifes.length - 1) sb.append(" | ");
        }

        BossBar bar = arena.getGameBar();
        bar.name(Component.translatable(sb.toString()));
        bar.progress((seconds * 1000F) / roundProcessTime.toMillis());
        bar.color(BossBar.Color.WHITE);

        if (seconds % 10 == 0 || seconds <= 3)
            arena.sendMessage(Messages.GAME_ENDING.toTranslatable(Component.text(seconds)));

        seconds--;
    }
}
