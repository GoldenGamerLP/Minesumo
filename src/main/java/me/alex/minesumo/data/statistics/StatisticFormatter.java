package me.alex.minesumo.data.statistics;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.events.ArenaEndEvent;
import me.alex.minesumo.messages.Messages;
import me.alex.minesumo.utils.ListUtils;
import me.alex.minesumo.utils.MojangUtils;
import me.alex.minesumo.utils.TimeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StatisticFormatter {

    private final StatisticDB handler;

    public StatisticFormatter(Minesumo minesumo) {
        this.handler = minesumo.getStatsHandler();
    }

    //Arena Statistics from gameID and with a list of death history
    public CompletableFuture<Component> getGameStatistics(String gameID) {
        if (!MojangUtils.isValidGameID(gameID)) return CompletableFuture
                .completedFuture(Messages.GENERAL_NOT_FOUND.toTranslatable());

        return this.handler.arenaExists(gameID).thenApply(exists -> {
            if (!exists) return Messages.GENERAL_NOT_FOUND.toTranslatable();

            return this.handler.getArenaCache().get(gameID).thenApply(this::formatGameStats).join();
        });
    }


    //Player statistics from minecraft name
    public CompletableFuture<Component> getPlayerStatistics(String name) {
        if (!MojangUtils.isValidName(name)) return CompletableFuture
                .completedFuture(Messages.GENERAL_NOT_FOUND.toTranslatable());

        return this.handler.playerExists(name).thenApply(exists -> {
            if (!exists) return Messages.GENERAL_NOT_FOUND.toTranslatable();

            return this.handler.getPlayerStatistics(name).thenApply(this::formatPlayerStats).join();
        });
    }

    public CompletableFuture<Component> getPlayerStatistics(@Nullable UUID uuid) {
        if (uuid == null) return CompletableFuture
                .completedFuture(Messages.GENERAL_NOT_FOUND.toTranslatable());

        return this.handler.playerExists(uuid).thenApply(exists -> {
            if (!exists) return Messages.GENERAL_NOT_FOUND.toTranslatable();

            return this.handler.getPlayerCache().get(uuid).thenApply(this::formatPlayerStats).join();
        });
    }

    @Blocking
        //Format arena statistics to a component from the Messages and use variables
    Component formatGameStats(ArenaStatistics statistics) {
        Map<UUID, PlayerStatistics> participants = handler.getBulkPlayers(statistics.getParticipants())
                .join();
        List<PlayerStatistics> pls = List.copyOf(participants.values());
        List<PlayerStatistics> winner = new ArrayList<>();
        statistics.getWinners().forEach(uuid -> winner.add(participants.get(uuid)));

        String gameID = statistics.getSessionID();
        String players = ListUtils.formatList(pls, PlayerStatistics::getLastName);
        String winners = ListUtils.formatList(winner, PlayerStatistics::getLastName);
        String duration = TimeUtils.formatTime(statistics.getStart(), statistics.getStop());
        String start = TimeUtils.formatDate(statistics.getStart(), Locale.getDefault());
        String endState = statistics.getEndState().name();
        TextColor color = statistics.getEndState() == ArenaEndEvent.EndState.WIN ?
                TextColor.color(0xFF1C) : TextColor.color(0xFF005D);


        TextComponent.Builder deathAndKillHistory = Component.text().appendNewline();
        //go through the death history and format it
        for (int i = 0; i < statistics.getKillsAndDeathHistory().size(); i++) {
            ArenaStatistics.KillAndDeathHistory history = statistics.getKillsAndDeathHistory().get(i);

            String killer = history.getAttacker() == null ?
                    "NaN" : participants.get(history.getAttacker()).getLastName();
            String victim = participants.get(history.getDead()).getLastName();

            deathAndKillHistory
                    .append(Component.text(" "))
                    .append(Messages.GENERAL_STATS_ARENA_HISTORY
                            .toTranslatable(
                                    Component.text(i + 1).color(NamedTextColor.GOLD),
                                    Component.text(victim).color(NamedTextColor.YELLOW),
                                    Component.text(killer).color(NamedTextColor.YELLOW)
                            ))
                    .appendNewline();
        }

        return Messages.GENERAL_STATS_ARENA.toTranslatable(
                Component.text(gameID).color(TextColor.color(0xFFC208)),
                Component.text(players).color(NamedTextColor.YELLOW),
                Component.text(start).color(NamedTextColor.YELLOW),
                Component.text(duration).color(NamedTextColor.YELLOW),
                Component.text(endState, color),
                Component.text(winners).color(NamedTextColor.YELLOW),
                deathAndKillHistory.build());
    }


    Component formatPlayerStats(PlayerStatistics playerStats) {
        String name = playerStats.getLastName();
        int kills = playerStats.getKills();
        int death = playerStats.getDeaths();
        int gamesPlayed = playerStats.getGamesPlayed().size();
        int gamesWon = playerStats.getWins();
        int loses = gamesPlayed - gamesWon;
        double kd = (double) kills / (double) death;
        double wl = (double) gamesWon / (double) gamesPlayed;
        String lastPlayedGame = playerStats.getGamesPlayed().size() == 0 ?
                "NaN" : playerStats.getGamesPlayed().get(0);

        return Messages.GENERAL_STATS_PLAYER.toTranslatable(
                Component.text(name).color(TextColor.color(0xFFC208)),
                Component.text(kills).color(NamedTextColor.YELLOW),
                Component.text(death).color(NamedTextColor.YELLOW),
                Component.text(String.format("%1.2f", kd)).color(NamedTextColor.YELLOW),
                Component.text(gamesWon).color(NamedTextColor.YELLOW),
                Component.text(loses).color(NamedTextColor.YELLOW),
                Component.text(String.format("%1.2f", wl)).color(NamedTextColor.YELLOW),
                Component.text(gamesPlayed).color(NamedTextColor.YELLOW),
                Component.text(lastPlayedGame).color(NamedTextColor.YELLOW)
        );
    }
}
