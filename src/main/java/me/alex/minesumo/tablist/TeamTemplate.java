package me.alex.minesumo.tablist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TeamTemplate {

    protected final Component prefix, suffix;
    protected final NamedTextColor color;
    protected final int priority;

    public TeamTemplate() {
        this.prefix = Component.empty();
        this.suffix = Component.empty();
        this.color = NamedTextColor.WHITE;
        this.priority = 0;
    }

    public TeamTemplate(Component prefix, Component suffix, NamedTextColor color, int priority) {
        this.prefix = prefix == null ? Component.empty() : prefix;
        this.suffix = suffix == null ? Component.empty() : suffix;
        this.color = color == null ? NamedTextColor.WHITE : color;
        this.priority = priority;
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

    public int getPriority() {
        return priority;
    }

    public static final class TeamBuilder {
        private Component prefix;
        private Component suffix;
        private NamedTextColor color;

        private int priority;

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

        public TeamBuilder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public TeamTemplate build() {
            return new TeamTemplate(prefix, suffix, color, priority);
        }
    }
}
