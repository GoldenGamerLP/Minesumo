package me.alex.minesumo.data.statistics;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.events.ArenaEndEvent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class StatisticsManager {

    private final StatisticDB handler;

    public StatisticsManager(Minesumo minesumo) {
        this.handler = minesumo.getStatsHandler();
    }

    public void startGame(String gameID, List<Player> participants) {
        List<UUID> pls = participants.stream().map(Player::getUuid).toList();
        this.handler.getArenaCache().get(gameID).thenAccept(statistics -> {
            statistics.getParticipants().addAll(pls);
            statistics.setStart(Date.from(Instant.now()));
        });

        pls.forEach(uuid -> this.handler
                .getPlayerCache().get(uuid)
                .thenAccept(statistics -> statistics.getGamesPlayed().add(0, gameID)));
    }

    public void addDeath(String gameID, UUID player, @Nullable UUID attacker) {
        this.handler.getPlayerCache().get(player).thenAccept(playerStatistics -> {
            playerStatistics.setDeaths(playerStatistics.getDeaths() + 1);
        });

        if (attacker != null) this.handler.getPlayerCache().get(attacker).thenAccept(playerStatistics -> {
            playerStatistics.setKills(playerStatistics.getKills() + 1);
        });

        this.handler.getArenaCache().get(gameID).thenAccept(statistics -> {
            ArenaStatistics.KillAndDeathHistory hs = new ArenaStatistics.KillAndDeathHistory(
                    player,
                    attacker,
                    Date.from(Instant.now()));

            statistics.getKillsAndDeathHistory().add(0, hs);
        });
    }

    public void arenaEnd(String gameID, ArenaEndEvent.EndState state, List<Player> players) {
        List<UUID> pls = players.stream().map(Player::getUuid).toList();
        this.handler.getArenaCache().get(gameID).thenAccept(statistics -> {
            statistics.setEndState(state);
            statistics.getWinners().addAll(pls);
            statistics.setStop(Date.from(Instant.now()));
        });

        pls.forEach(uuid -> this.handler
                .getPlayerCache()
                .get(uuid)
                .thenAccept(statistics -> statistics.setWins(statistics.getWins() + 1)));
    }
}
