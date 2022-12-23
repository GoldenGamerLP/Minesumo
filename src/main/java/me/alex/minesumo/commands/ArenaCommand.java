package me.alex.minesumo.commands;

import me.alex.minesumo.data.instances.ArenaImpl;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaCommand extends Command {

    public ArenaCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        var arg = ArgumentType.String("isArena");

        var playerArg = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        this.addSyntax((sender, commandString) -> {
            if (sender instanceof Player player) {
                if (player.getInstance() instanceof ArenaImpl arenaImpl) {
                    player.sendMessage("Du bist in einer Arena!");
                } else player.sendMessage("Bist in keiner Arena!");
            }
        }, arg);
    }
}
