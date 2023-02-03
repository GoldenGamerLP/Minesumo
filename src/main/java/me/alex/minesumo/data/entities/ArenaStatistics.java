package me.alex.minesumo.data.entities;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.Data;
import me.alex.minesumo.events.ArenaEndEvent;

import java.util.*;

@Data
public final class ArenaStatistics {

    final String sessionID;

    final List<UUID> participants;
    final List<KillAndDeathHistory> killsAndDeathHistory;
    final Map<Integer,List<UUID>> teams;
    final List<UUID> winners;

    Date start, stop;

    ArenaEndEvent.EndState endState;

    public ArenaStatistics(String uid) {
        this.sessionID = uid;

        this.participants = new ArrayList<>();
        this.killsAndDeathHistory = new LinkedList<>();
        this.winners = new ArrayList<>();
        this.teams = new Int2ReferenceOpenHashMap<>(6);
    }

    @Data
    public static final class KillAndDeathHistory {
        final UUID dead;

        final UUID attacker;

        final Date time;
    }
}
