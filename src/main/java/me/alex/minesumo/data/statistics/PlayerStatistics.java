package me.alex.minesumo.data.statistics;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public final class PlayerStatistics {

    private final UUID playerID;
    private final Map<UUID, Instant> gamesPlayed;
    private int kills, deaths;

    public PlayerStatistics(UUID playerID) {
        this.playerID = playerID;
        this.gamesPlayed = new HashMap<>();
        this.kills = 0;
        this.deaths = 0;
    }
}
