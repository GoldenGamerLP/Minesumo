package me.alex.minesumo.utils;

import net.minestom.server.MinecraftServer;

public record LegacyKnockbackSettings(double horizontal, double vertical,
                                      double verticalLimit,
                                      double extraHorizontal, double extraVertical) {
    public static final LegacyKnockbackSettings DEFAULT = builder().build();

    public LegacyKnockbackSettings(double horizontal, double vertical, double verticalLimit,
                                   double extraHorizontal, double extraVertical) {
        double tps = MinecraftServer.TICK_PER_SECOND;
        this.horizontal = horizontal * tps;
        this.vertical = vertical * tps;
        this.verticalLimit = verticalLimit * tps;
        this.extraHorizontal = extraHorizontal * tps;
        this.extraVertical = extraVertical * tps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double horizontal = 0.4, vertical = 0.4;
        private double verticalLimit = 0.4;
        private double extraHorizontal = 0.5, extraVertical = 0.1;

        public Builder horizontal(double horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        public Builder vertical(double vertical) {
            this.vertical = vertical;
            return this;
        }

        public Builder verticalLimit(double verticalLimit) {
            this.verticalLimit = verticalLimit;
            return this;
        }

        public Builder extraHorizontal(double extraHorizontal) {
            this.extraHorizontal = extraHorizontal;
            return this;
        }

        public Builder extraVertical(double extraVertical) {
            this.extraVertical = extraVertical;
            return this;
        }

        public LegacyKnockbackSettings build() {
            return new LegacyKnockbackSettings(horizontal, vertical, verticalLimit, extraHorizontal, extraVertical);
        }
    }
}
