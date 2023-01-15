package me.alex.minesumo.data.statistics;

import com.google.gson.annotations.Expose;
import lombok.Data;
import me.alex.minesumo.events.ArenaEndEvent;

import java.util.*;

@Data
public final class ArenaStatistics {

    @Expose
    private final String sessionID;

    @Expose
    private final List<UUID> participants;
    @Expose
    private final List<KillAndDeathHistory> killsAndDeathHistory;
    @Expose
    private final List<UUID> winners;
    @Expose
    private Date start, stop;
    @Expose
    private ArenaEndEvent.EndState endState;

    public ArenaStatistics(String uid) {
        this.sessionID = uid;

        this.participants = new ArrayList<>();
        this.killsAndDeathHistory = new LinkedList<>();
        this.winners = new ArrayList<>();
    }

    @Data
    public static class KillAndDeathHistory {
        @Expose
        private final UUID dead;

        @Expose
        private final UUID attacker;

        @Expose
        private final Date time;

        public KillAndDeathHistory(UUID player, UUID attacker, Date from) {
            this.dead = player;
            this.attacker = attacker;
            this.time = from;
        }
    }
}
