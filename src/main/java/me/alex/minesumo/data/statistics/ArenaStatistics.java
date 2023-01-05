package me.alex.minesumo.data.statistics;

import com.google.gson.annotations.Expose;
import kotlin.Pair;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

@Data
public final class ArenaStatistics {

    @Expose
    @Getter
    final UUID sessionID;

    @Expose
    @Getter
    List<UUID> participants;

    @Expose
    @Getter
    Instant start, stop;

    @Expose
    @Getter
    Map<Instant, Pair<UUID, UUID>> killsAndDeathHistory;

    public ArenaStatistics(UUID uuid) {
        this.sessionID = uuid;

        this.participants = new ArrayList<>();
        this.killsAndDeathHistory = new HashMap<>();
        this.start = Instant.now();
    }

    public ArenaStatistics close() {
        this.stop = Instant.now();
        return this;
    }
}
