package me.alex.minesumo.instances;

import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.statistics.ArenaStatistics;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.SharedInstance;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;

public abstract class AbstractArena extends SharedInstance {

    public static final Duration
            roundStartingTime = Duration.ofSeconds(5),
            roundProcessTime = Duration.ofMinutes(3),
            roundEndingTime = Duration.ofSeconds(10);
    protected final ArenaStatistics stats;

    protected final MapConfig config;
    protected Integer maxLives = null;

    public AbstractArena(@NotNull UUID uniqueId, @NotNull InstanceContainer instanceContainer, @NotNull MapConfig config) {
        super(uniqueId, instanceContainer);
        this.config = config;
        this.stats = new ArenaStatistics(uniqueId);
    }

    public Integer getMaxLives() {
        return maxLives == null ? config.getStartingLives() : maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }
}
