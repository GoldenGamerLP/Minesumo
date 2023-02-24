package me.alex.minesumo.tablist.prefixprovider;

import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.tablist.TeamTemplate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.minestom.server.entity.Player;

import java.util.function.BiConsumer;

public class LuckpermsPrefixProvider implements BiConsumer<Player, TeamTemplate.TeamBuilder> {

    private final LuckPerms luckPerms;
    private final StatisticDB statsHandler;
    private final MiniMessage miniMesssage = MiniMessage.miniMessage();

    public LuckpermsPrefixProvider(StatisticDB statsHandler) {
        this.luckPerms = LuckPermsProvider.get();
        this.statsHandler = statsHandler;
    }

    @Override
    public void accept(Player player, TeamTemplate.TeamBuilder teamBuilder) {
        if (luckPerms != null) {
            CachedMetaData data = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            //if data.getPrefix() is null, it will return an empty component
            teamBuilder.withPrefix(data.getPrefix() == null ? Component.empty() : miniMesssage.deserialize(data.getPrefix()));

        } else teamBuilder.withPrefix(Component.text(" "));

        //Black color
        teamBuilder.withColor(NamedTextColor.GRAY);

        long ranking = statsHandler.getPlayers().join() -
                (statsHandler.getPlayerRanking(player.getUuid()).join() - 1);

        Component suffix = Component.text(" | ").color(NamedTextColor.GRAY)
                .append(Component.text(ranking).color(NamedTextColor.YELLOW));

        teamBuilder.withSuffix(suffix);
    }
}
