package me.alex.minesumo.data.statistics;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
public final class PlayerStatistics {

    @Expose
    private final UUID playerID;
    @Expose
    private final List<String> gamesPlayed;
    @Expose
    private String lastName;
    @Expose
    private int
            kills = 0,
            deaths = 0,
            wins = 0;

    public PlayerStatistics(UUID playerID, String knownName) {
        this.playerID = playerID;
        this.lastName = knownName;
        this.gamesPlayed = new LinkedList<>();
    }
}

