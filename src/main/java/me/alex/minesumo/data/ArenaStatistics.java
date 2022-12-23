package me.alex.minesumo.data;

import com.google.gson.annotations.Expose;
import kotlin.Pair;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

class ArenaStatistics {

    @Expose
    @Getter
    final UUID sessionID;

    @Expose
    @Getter
    List<UUID> participants;

    @Expose
    @Getter
    Instant timestamp;

    @Expose
    @Getter
    Map<Instant, Pair<UUID, UUID>> killsAndDeathHistory;

    ArenaStatistics(UUID uuid) {
        this.sessionID = uuid;

        this.participants = new ArrayList<>();
        this.killsAndDeathHistory = new HashMap<>();
        this.timestamp = Instant.now();
    }
}
