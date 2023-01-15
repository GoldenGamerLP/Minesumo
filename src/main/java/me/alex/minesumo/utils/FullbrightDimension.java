package me.alex.minesumo.utils;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public class FullbrightDimension {

    public static final DimensionType DEFAULT_DIM = DimensionType.builder(NamespaceID.from("minesumo:full_bright"))
            .ambientLight(1.5F)
            .fixedTime(6000L)
            .minY(-64)
            .height(384)
            .logicalHeight(384)
            .natural(false)
            .skylightEnabled(false)
            .build();

    static {
        MinecraftServer.getDimensionTypeManager().addDimension(DEFAULT_DIM);
    }
}
