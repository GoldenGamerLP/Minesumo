package me.alex.minesumo.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class TablistPlayerChangeEvent implements PlayerEvent {
    private final Player player;
    private final Component prefix, suffix;
    private final NamedTextColor color;


    public TablistPlayerChangeEvent(Player player, Component prefix, Component suffix, NamedTextColor color) {
        this.player = player;
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Component getPrefix() {
        return prefix;
    }

    public Component getSuffix() {
        return suffix;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
