package me.alex.minesumo.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Team {

    protected final Component prefix, suffix;
    protected final NamedTextColor color;

    public Team() {
        this.prefix = Component.empty();
        this.suffix = Component.empty();
        this.color = NamedTextColor.WHITE;
    }

    public Team(Component prefix, Component suffix, NamedTextColor color) {
        this.prefix = prefix == null ? Component.empty() : prefix;
        this.suffix = suffix == null ? Component.empty() : suffix;
        this.color = color == null ? NamedTextColor.WHITE : color;
    }

    public Component getSuffix() {
        return suffix;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Component getPrefix() {
        return prefix;
    }


    public static final class TeamBuilder {
        private Component prefix;
        private Component suffix;
        private NamedTextColor color;

        private TeamBuilder() {
        }

        public static TeamBuilder newBuilder() {
            return new TeamBuilder();
        }

        public TeamBuilder withPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        public TeamBuilder withSuffix(Component suffix) {
            this.suffix = suffix;
            return this;
        }

        public TeamBuilder withColor(NamedTextColor color) {
            this.color = color;
            return this;
        }

        public Team build() {
            return new Team(prefix, suffix, color);
        }
    }
}
