package me.alex.minesumo.tasks;

import me.alex.minesumo.events.ArenaEndEvent;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import me.alex.minesumo.utils.StringUtils;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (seconds == 0)
            this.cancel();

        Integer[] lifes = arena.getLives();
        List<Integer> teams = arena.getLivingTeams();
        TextComponent.Builder builder = Component.text();

        for (Integer livingTeam : teams) {
            List<Player> players = arena.getPlayersFromTeam(livingTeam);
            String player = players.size() > 1 || teams.size() < 2 ?
                    players.size() + "" : StringUtils.getFirstXLetters(players.get(0).getUsername(), 9);
            String team = livingTeam + "";
            String lives = lifes[livingTeam] + "";

            builder.append(Messages.GAME_TEAM_ENTRY.toTranslatable(
                    Component.text(team).color(NamedTextColor.GOLD),
                    Component.text(lives).color(NamedTextColor.YELLOW),
                    Component.text(player).color(NamedTextColor.YELLOW)
            ));
            builder.append(Component.space());
        }

        BossBar bar = arena.getGameBar();
        bar.name(builder);
        bar.progress((seconds * 1000F) / roundProcessTime.toMillis());
        bar.color(BossBar.Color.WHITE);

        if (seconds % 10 == 0 || seconds <= 3)
            arena.sendMessage(Messages.GAME_ENDING.toTranslatable(Component.text(seconds)));

        seconds--;
    }

    @Override
    void onStop() {
        if (seconds != 0) return;
        EventDispatcher.call(new ArenaEndEvent(arena, ArenaEndEvent.EndState.DRAW, arena.getPlayerFromState(ArenaImpl.PlayerState.ALIVE), 0));
        arena.changeArenaState(ArenaImpl.ArenaState.ENDING);
    }
}
