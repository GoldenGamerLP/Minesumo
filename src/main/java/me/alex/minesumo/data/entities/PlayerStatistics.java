package me.alex.minesumo.data.entities;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
public final class PlayerStatistics {
    final UUID playerID;
    List<String> gamesPlayed;
    String lastName;
    int
            kills = 0,
            deaths = 0,
            wins = 0;

    public PlayerStatistics(UUID playerID, String knownName) {
        this.playerID = playerID;
        this.lastName = knownName;
        this.gamesPlayed = new LinkedList<>();
    }
}

