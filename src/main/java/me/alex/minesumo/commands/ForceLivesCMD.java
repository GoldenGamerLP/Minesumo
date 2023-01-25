package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;

public class ForceLivesCMD extends Command {

    public ForceLivesCMD(Minesumo minesumo) {
        super("lives", "force-lives", "maxLifes");

        setCondition(Conditions::playerOnly);

        int max = minesumo.getConfig().getMaxLives();
        int min = minesumo.getConfig().getMinLives();
        var lives = ArgumentType.Integer("lives")
                .between(min, max);


        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl arena)) {
                player.sendMessage(Messages.GENERAL_NOT_IN_AN_ARENA.toTranslatable());
                return;
            }

            ArenaImpl.ArenaState state = arena.getState();
            if (!(state == ArenaImpl.ArenaState.WAITING_FOR_PLAYERS || state == ArenaImpl.ArenaState.NORMAL_STARTING)) {
                player.sendMessage(Messages
                        .GAME_COMMAND_WRONG_ARENA_STATE
                        .toTranslatable());
                return;
            }

            Integer integer = context.get(lives);
            if (integer == null) return;

            arena.setMaxLives(integer);
            arena.sendMessage(Messages
                    .GAME_COMMAND_SET_LIVES
                    .toTranslatable(Component.text(integer)));
        }, lives);

        MinecraftServer.getCommandManager().register(this);
    }
}
