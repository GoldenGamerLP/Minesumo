package me.alex.minesumo.instances;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.utils.FullbrightDimension;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;

import java.time.Duration;
import java.util.UUID;

@Slf4j
public class MinesumoInstance extends InstanceContainer {

    private final MapConfig config;

    public MinesumoInstance(MapConfig config) {
        super(UUID.randomUUID(), FullbrightDimension.DEFAULT_DIM);

        this.config = config;
        //
        basicArenaSetup();
        registerBasicListener();
        //Register
        MinecraftServer.getInstanceManager().registerInstance(this);

        log.info("Registered Map {}", config.getSchematicFile());
    }

    private void basicArenaSetup() {
        this.setTimeUpdate(Duration.ofSeconds(1));
        this.setTime(0);
        this.setTimeRate(0);

        this.getWorldBorder().setCenter(0, 0);
        this.getWorldBorder().setDiameter(this.config.getMapSchematic().getLength());
        this.getWorldBorder().setWarningBlocks(1);
    }

    private void registerBasicListener() {
    }

    public MapConfig getConfig() {
        return config;
    }

    public ArenaImpl createCopy() {
        return new ArenaImpl(this, this.config);
    }
}
