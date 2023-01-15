package me.alex.minesumo.commands;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Player;

@Slf4j
public class ForceStartCMD extends Command {

    private final Integer MIN_PLAYER_START;

    public ForceStartCMD(Minesumo minesumo) {
        super("start", "force-start", "skip");

        this.MIN_PLAYER_START = minesumo.getConfig().getMinPlayersToStart();

        setCondition(Conditions::playerOnly);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl arena)) {
                player.sendMessage(Messages.GENERAL_NOT_IN_AN_ARENA.toTranslatable());
                return;
            }

            if (arena.getState() != ArenaImpl.ArenaState.WAITING_FOR_PLAYERS) {
                player.sendMessage(Messages
                        .GAME_COMMAND_WRONG_ARENA_STATE
                        .toTranslatable());
                return;
            }

            if (arena.getPlayers(ArenaImpl.PlayerState.ALIVE).size() < 2) {
                player.sendMessage(Messages
                        .GAME_COMMAND_FAILURE_PLAYERS
                        .toTranslatable(Component.text(MIN_PLAYER_START)));
                return;
            }

            //Starting Arena
            log.info("Force Starting Arena {}", arena.getGameID());
            player.sendMessage(Messages
                    .GAME_COMMAND_FORCE_START_SUCCESS
                    .toTranslatable());
            arena.changeArenaState(ArenaImpl.ArenaState.NORMAL_STARTING);
        });

        MinecraftServer.getCommandManager().register(this);
    }
}
