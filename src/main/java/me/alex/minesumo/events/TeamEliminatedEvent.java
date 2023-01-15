package me.alex.minesumo.events;

import me.alex.minesumo.instances.ArenaImpl;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class TeamEliminatedEvent implements InstanceEvent, PlayerEvent {

    private final Instance instance;
    private final int teamID;
    private final Player lastDeathPlayerOfTeam, attacker;

    public TeamEliminatedEvent(ArenaImpl instance, int teamID, Player lastDeathPlayerOfTeam, Player attacker) {
        this.instance = instance;
        this.teamID = teamID;
        this.lastDeathPlayerOfTeam = lastDeathPlayerOfTeam;
        this.attacker = attacker;
    }

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }

    public int getTeamID() {
        return teamID;
    }

    public Player getLastDeathPlayerOfTeam() {
        return lastDeathPlayerOfTeam;
    }

    @Override
    public @NotNull Player getPlayer() {
        return lastDeathPlayerOfTeam;
    }

    public Player getAttacker() {
        return attacker;
    }
}
