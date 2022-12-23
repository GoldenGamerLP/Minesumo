package me.alex.minesumo.data;

import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.SharedInstance;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class Arena extends SharedInstance {

    protected final ArenaStatistics stats;

    public Arena(@NotNull UUID uniqueId, @NotNull InstanceContainer instanceContainer) {
        super(uniqueId, instanceContainer);
        stats = new ArenaStatistics(uniqueId);
    }
}
