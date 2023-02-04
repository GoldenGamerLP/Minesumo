package me.alex.minesumo.instances;

import lombok.Getter;
import me.alex.minesumo.data.entities.MapConfig;
import net.minestom.server.instance.SharedInstance;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;

public abstract class AbstractArena extends SharedInstance {

    public static final Duration
            roundStartingTime = Duration.ofSeconds(5),
            roundProcessTime = Duration.ofMinutes(5),
            roundEndingTime = Duration.ofSeconds(25);
    @Getter
    protected final MapConfig config;
    @Getter
    private final String gameID;
    private Integer maxLives = 0;

    public AbstractArena(@NotNull UUID uniqueId, @NotNull MinesumoInstance instanceContainer, @NotNull MapConfig config, String gameID) {
        super(uniqueId, instanceContainer);
        this.config = config;
        this.gameID = gameID;
    }

    public Integer getMaxLives() {
        return maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }
}
