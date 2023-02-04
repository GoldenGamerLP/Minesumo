package me.alex.minesumo.tasks;

import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;

import java.util.List;

import static me.alex.minesumo.instances.AbstractArena.roundStartingTime;

public class RoundStartingTask extends AbstractTask {

    private long seconds;

    public RoundStartingTask(ArenaImpl arena) {
        super(arena);

        this.seconds = roundStartingTime.toSeconds();
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.NORMAL_STARTING) {
            this.cancel();
            return;
        }

        if (seconds == roundStartingTime.toSeconds()) {


            arena.getLivingTeams().forEach(integer -> {
                //Todo: Add history for teams
                List<String> players = arena.getPlayersFromTeam(integer).stream().map(Player::getUsername).toList();
                //TODO: print updates teams
            });
        }
        if (seconds == 0) {
            arena.addPlayersToTeam();

            arena.getPlayers().forEach(arena::teleportPlayerToSpawn);
            arena.getPlayerFromState(ArenaImpl.PlayerState.SPECTATOR).forEach(arena::makePlayerSpectator);
            arena.changeArenaState(ArenaImpl.ArenaState.INGAME);
        }

        float percent = seconds * 1F / roundStartingTime.toSeconds();
        Component component = Messages.GAME_STARTING.toTranslatable(Component.text(seconds));


        if (seconds <= 3) {
            Title title = Title.title(
                    Messages.GAME_STARTING_TITLE.toTranslatable()
                            .color(TextColor.lerp(percent, NamedTextColor.GREEN, NamedTextColor.RED)),
                    Component.text(seconds));
            arena.showTitle(title);
        }

        BossBar bar = arena.getGameBar();
        bar.name(component);
        bar.progress(percent);
        bar.color(BossBar.Color.GREEN);

        arena.sendActionBar(component);

        seconds--;
    }

    @Override
    void onStop() {
        arena.clearTitle();
        arena.resetTitle();
    }
}
