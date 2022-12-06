package me.alex.minesumo.data.instances;

import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.events.PlayerArenaDeathEvent;
import me.alex.minesumo.utils.DefaultInstanceSettings;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.InstanceContainer;

import java.time.Duration;
import java.util.UUID;

// TODO: Remove this class and only use eventnode
public class MinesumoInstance extends InstanceContainer {

    private final MapConfig config;

    public MinesumoInstance(MapConfig config) {
        super(UUID.randomUUID(), DefaultInstanceSettings.DEFAULT_DIM);

        this.config = config;
        //
        basicArenaSetup();
        registerBasicListener();
        //Register
        MinecraftServer.getInstanceManager().registerInstance(this);
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
        this.eventNode().addListener(PlayerMoveEvent.class, playerMoveEvent -> {
            if (playerMoveEvent.isOnGround()) return;

            if (playerMoveEvent.getNewPosition().y() < this.config.getDeathLevel() ||
                    !this.getWorldBorder().isInside(playerMoveEvent.getNewPosition())) {



            }
        });
    }

    public Arena createCopy() {
        return new Arena(this, this.config);
    }
}
