package me.alex.minesumo.tasks;

import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;

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

        List<Player> players = arena.getPlayerFromState(ArenaImpl.PlayerState.ALIVE);
        //A list of players that are still loading or in a loading screen or did not answer a keep alive packet
        List<Player> waitingPlayers = players.stream().filter(player ->
                        !player.isOnline() ||
                                !player.isActive() ||
                                !player.didAnswerKeepAlive() ||
                                !(player.getPlayerConnection().getConnectionState() == ConnectionState.PLAY))
                .toList();

        if (waitingPlayers.isEmpty() && players.size() == arena.getMaxPlayers()) {
            arena.changeArenaState(ArenaImpl.ArenaState.NORMAL_STARTING);
            this.cancel();
        }

        int maxPlayers = arena.getMaxPlayers();
        float percentage = players.size() * 1F / maxPlayers;
        int needed = maxPlayers - players.size();

        Component bossBar = !waitingPlayers.isEmpty() ?
                Messages.GAME_WAITING_PLAYER_LOADING.toTranslatable(Component.text(waitingPlayers.size())) :
                Messages.GAME_WAITING_PLAYERS.toTranslatable(Component.text(needed));

        BossBar bar = arena.getGameBar();
        bar.progress(percentage);
        bar.name(bossBar);
        bar.color(BossBar.Color.YELLOW);

        arena.sendActionBar(Messages.GAME_WAITING.toTranslatable());
    }
}
