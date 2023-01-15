package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;

public class GameIdCMD extends Command {

    public GameIdCMD(Minesumo minesumo) {
        super("gameid", "game-id");

        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;

            if (!(player.getInstance() instanceof ArenaImpl impl)) {
                player.sendMessage(Messages.GENERAL_NOT_IN_AN_ARENA.toTranslatable());
                return;
            }

            player.sendMessage(Messages.GAME_COMMAND_GAME_ID
                    .toTranslatable(
                            Component.text(impl.getGameID()).color(NamedTextColor.YELLOW)
                    ));
        });

        MinecraftServer.getCommandManager().register(this);
    }
}
