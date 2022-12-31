package me.alex.minesumo.data.tasks;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.TitlePart;
import net.minestom.server.entity.Player;

import java.util.List;

import static me.alex.minesumo.data.instances.ArenaImpl.roundStartingTime;

public class RoundStartingTask extends AbstractTask {

    private volatile long seconds;

    public RoundStartingTask(ArenaImpl arena) {
        super(arena);

        this.seconds = roundStartingTime.toSeconds();
    }

    @Override
    void onRun(ArenaImpl arena) {
        if (arena.getState() != ArenaImpl.ArenaState.NORMAL_STARTING){
            this.cancel();
            return;
        }

        if (seconds == roundStartingTime.toSeconds()) {
            arena.addPlayersToTeam();
            arena.getPlayers().forEach(arena::teleportPlayerToSpawn);

            arena.getLivingTeams().forEach(integer -> {
                List<String> players = arena.getPlayers(integer).stream().map(Player::getUsername).toList();
                arena.sendMessage(Component.translatable("Team: " + integer + " | Players: " + players));
            });
        }
        if (seconds == 0)
            arena.changeArenaState(ArenaImpl.ArenaState.INGAME);




        Component component = Component.translatable("starting.in");
        if (seconds <= 3) arena.getPlayers(ArenaImpl.PlayerState.ALIVE).forEach(arenaPlayer -> {
            arenaPlayer.sendTitlePart(TitlePart.TITLE, component);
            arenaPlayer.sendTitlePart(TitlePart.SUBTITLE, Component.text(seconds));
        });

        BossBar bar = arena.getGameBar();
        bar.name(Component.translatable("starting.in" + seconds));
        bar.progress(seconds * 1F / roundStartingTime.toSeconds());

        arena.sendMessage(Component.translatable("starting.in" + seconds));

        seconds--;
    }
}
