package me.alex.minesumo.data.configuration;

public class ArenaConfig extends MapConfig {

    private final MapConfig mapConfig;
    private final int rounds;
    int gewicht = 85;

    public ArenaConfig(MapConfig mapConfig, int rounds) {
        this.mapConfig = mapConfig;
        this.rounds = rounds;
    }
}
