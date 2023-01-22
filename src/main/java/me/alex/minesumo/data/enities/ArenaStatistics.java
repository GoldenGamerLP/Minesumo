package me.alex.minesumo.data.enities;

import dev.morphia.annotations.*;
import lombok.Data;
import me.alex.minesumo.events.ArenaEndEvent;

import java.util.*;

@Data
@Entity("arena_statistics")
@Indexes(@Index(fields = @Field(value = "sessionID")))
public final class ArenaStatistics {

    @Id
    String sessionID;

    List<UUID> participants;
    List<KillAndDeathHistory> killsAndDeathHistory;
    List<UUID> winners;
    Date start, stop;
    ArenaEndEvent.EndState endState;

    public ArenaStatistics(String uid) {
        this.sessionID = uid;

        this.participants = new ArrayList<>();
        this.killsAndDeathHistory = new LinkedList<>();
        this.winners = new ArrayList<>();
    }

    @Data
    public static class KillAndDeathHistory {
        final UUID dead;
        final UUID attacker;
        final Date time;

        public KillAndDeathHistory(UUID player, UUID attacker, Date from) {
            this.dead = player;
            this.attacker = attacker;
            this.time = from;
        }
    }
}
