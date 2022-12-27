package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.instances.ArenaImpl;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ArenaCommand extends Command {

    public ArenaCommand(@NotNull String name, Minesumo minesumo) {
        super(name);

        var arg = ArgumentType.Literal("isArena");
        var playerArg = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        var change = ArgumentType.Literal("change");
        var changeEnum = ArgumentType.Enum("state", ArenaImpl.ArenaState.class);

        this.addSyntax((sender, commandString) -> {
            Player player;
            if (commandString.has(playerArg))
                player = commandString.get(playerArg).findFirstPlayer(sender);
            else if (sender instanceof Player p)
                player = p;
            else {
                sender.sendMessage("You arent a player.");
                return;
            }

            if (!(player.getInstance() instanceof ArenaImpl impl)) {
                sender.sendMessage("You are not in an arena.");
                return;
            }
            sender.sendMessage("You are in a arena! + " + impl.getMapConfig().getSchematicFile());
        }, arg, playerArg);


        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl impl)) return;

            ArenaImpl.ArenaState state = context.get(changeEnum);

            if (state == null) return;

            player.sendMessage("Updating!");
            impl.changeArenaState(state);
        }, change, changeEnum);

        MinecraftServer.getCommandManager().register(this);
    }
}
