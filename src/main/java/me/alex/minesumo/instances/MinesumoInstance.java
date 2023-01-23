package me.alex.minesumo.instances;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.data.entities.MapConfig;
import me.alex.minesumo.utils.FullbrightDimension;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;

import java.util.UUID;

@Slf4j
public class MinesumoInstance extends InstanceContainer {

    private final MapConfig config;

    public MinesumoInstance(MapConfig config) {
        super(UUID.randomUUID(), FullbrightDimension.DEFAULT_DIM);

        this.config = config;

        basicArenaSetup();

        //Register
        MinecraftServer.getInstanceManager().registerInstance(this);

        log.info("Registered Map {}", config.getSchematicFile());
    }

    private void basicArenaSetup() {
        this.setTime(6000);
        this.setTimeRate(0);
        this.setTimeUpdate(null);
    }

    public MapConfig getConfig() {
        return config;
    }

    public ArenaImpl createCopy(String gameID) {
        return new ArenaImpl(this, this.config, gameID);
    }
}
