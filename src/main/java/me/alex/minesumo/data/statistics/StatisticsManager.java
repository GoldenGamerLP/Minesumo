package me.alex.minesumo.data.statistics;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.data.entities.ArenaStatistics;
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

        this.handler.editArenaStats(gameID, stats -> {
            stats.setStart(Date.from(Instant.now()));
            stats.getParticipants().addAll(pls);
        });

        this.handler.editPlayerStats(pls, stats -> {
            //First Index because its latest arena
            stats.getGamesPlayed().add(0, gameID);
        });
    }

    public void addDeath(String gameID, UUID player, @Nullable UUID attacker) {
        this.handler.editPlayerStats(player, stats -> {
            stats.setDeaths(stats.getDeaths() + 1);
        });

        if (attacker != null) this.handler.editPlayerStats(attacker, stats -> {
            stats.setKills(stats.getKills() + 1);
        });

        this.handler.editArenaStats(gameID, stats -> {
            ArenaStatistics.KillAndDeathHistory history = new ArenaStatistics.KillAndDeathHistory(
                    player,
                    attacker,
                    Date.from(Instant.now())
            );

            stats.getKillsAndDeathHistory().add(0, history);
        });
    }

    public void arenaEnd(String gameID, ArenaEndEvent.EndState state, List<Player> players) {
        List<UUID> pls = players.stream().map(Player::getUuid).toList();
        this.handler.editPlayerStats(pls, stats -> {
            stats.setWins(stats.getWins() + 1);
        });

        this.handler.editArenaStats(gameID, stats -> {
            stats.setStop(Date.from(Instant.now()));
            stats.getWinners().addAll(pls);
            stats.setEndState(state);
        });
    }
}
