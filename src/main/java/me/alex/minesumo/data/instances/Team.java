package me.alex.minesumo.data.instances;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.minestom.server.adventure.audience.PacketGroupingAudience;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static net.kyori.adventure.identity.Identity.UUID;

public class Team implements PacketGroupingAudience {

    private final Player player;
    private volatile


    @Override
    public @NotNull Collection<@NotNull Player> getPlayers() {
        return null;
    }
}
