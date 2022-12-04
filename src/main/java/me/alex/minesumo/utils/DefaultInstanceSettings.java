package me.alex.minesumo.utils;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public class DefaultInstanceSettings {

    public static final DimensionType DEFAULT_DIM;

    static {
        NamespaceID namespaceID = NamespaceID.from("minesumo:dimension");
        DEFAULT_DIM = DimensionType.builder(namespaceID)
                .ambientLight(2F)
                .fixedTime(6000L)
                .minY(-64)
                .height(384)
                .logicalHeight(384)
                .natural(false)
                .skylightEnabled(false)
                .build();

        MinecraftServer.getDimensionTypeManager().addDimension(DEFAULT_DIM);
    }
}
