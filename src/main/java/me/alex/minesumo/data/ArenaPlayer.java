package me.alex.minesumo.data;

import com.google.gson.annotations.Expose;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArenaPlayer extends Player {

    @Expose
    private final UUID uuid;

    @Expose
    private Integer deaths, kills;

    @Expose
    private List<UUID> lastPlayedGames;

    public ArenaPlayer(@NotNull UUID uuid, @NotNull String username, @NotNull PlayerConnection playerConnection) {
        super(uuid, username, playerConnection);

        this.uuid = uuid;

        this.deaths = 0;
        this.kills = 0;

        this.lastPlayedGames = new ArrayList<>();
    }

    public void addHistory(ArenaStatistics data) {
        if (this.lastPlayedGames.contains(data.sessionID)) throw new IllegalStateException("Multiple arenas");

        this.lastPlayedGames.add(data.getSessionID());
    }

    public void addKill() {
        this.kills += 1;
    }

    public void addDeath() {
        this.deaths += 1;
    }


}
