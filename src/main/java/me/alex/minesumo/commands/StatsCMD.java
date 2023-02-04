package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.statistics.StatisticFormatter;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class StatsCMD extends Command {

    private final StatisticFormatter handler;

    public StatsCMD(Minesumo minesumo) {
        super("stats", "statistics");

        this.handler = minesumo.getStatisticFormatter();

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Use: /stats <player|gameID>"));
                return;
            }

            handler.getPlayerStatistics(player.getUuid()).thenAccept(player::sendMessage);
        });

        var allTimeStats = ArgumentType.Literal("top");
        var limit = ArgumentType.Integer("limit").between(1, 15).setDefaultValue(10);
        addSyntax((sender, context) -> {
            int limitValue = context.get(limit);
            this.handler.getTopPlayers(limitValue).whenComplete((component, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    return;
                }

                sender.sendMessage(component);
            });
        }, allTimeStats, limit);

        //GameID subcommand
        var selectID = ArgumentType.Literal("game");
        var gameID = ArgumentType.String("gameID");
        addSyntax((sender, context) -> {
            String game = context.get(gameID);

            handler.getGameStatistics(game).thenAccept(sender::sendMessage).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                }
            });
        }, selectID, gameID);

        //PlayerName subcommand
        var playerName = ArgumentType.String("playerName");
        addSyntax((sender, context) -> {
            String name = context.get(playerName);

            handler.getPlayerStatistics(name).thenAccept(sender::sendMessage);
        }, playerName);

        MinecraftServer.getCommandManager().register(this);
    }
}
