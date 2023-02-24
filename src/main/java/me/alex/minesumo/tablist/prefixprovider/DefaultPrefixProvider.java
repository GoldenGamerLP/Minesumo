package me.alex.minesumo.tablist.prefixprovider;

import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.tablist.TeamTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

import java.util.function.BiConsumer;

public class DefaultPrefixProvider implements BiConsumer<Player, TeamTemplate.TeamBuilder> {

    private final StatisticDB statsHandler;

    public DefaultPrefixProvider(StatisticDB statsHandler) {
        this.statsHandler = statsHandler;
    }

    @Override
    public void accept(Player player, TeamTemplate.TeamBuilder teamBuilder) {
        teamBuilder.withPrefix(Component.empty()).withColor(NamedTextColor.GRAY);

        long ranking = statsHandler.getPlayers().join() -
                (statsHandler.getPlayerRanking(player.getUuid()).join() - 1);

        Component suffix = Component.text(" | ").color(NamedTextColor.GRAY)
                .append(Component.text(ranking).color(NamedTextColor.YELLOW));

        teamBuilder.withSuffix(suffix);
    }
}
