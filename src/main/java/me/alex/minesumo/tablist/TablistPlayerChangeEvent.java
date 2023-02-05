package me.alex.minesumo.tablist;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TablistPlayerChangeEvent implements PlayerEvent {
    private final Player player;
    private final Consumer<Team.TeamBuilder> teamBuilder;


    public TablistPlayerChangeEvent(Player player, Consumer<Team.TeamBuilder> teamBuilder) {
        this.player = player;
        this.teamBuilder = teamBuilder;
    }

    public Consumer<Team.TeamBuilder> getTeamBuilder() {
        return teamBuilder;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }
}
