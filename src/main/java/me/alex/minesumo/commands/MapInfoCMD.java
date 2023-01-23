package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.entities.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.messages.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class MapInfoCMD extends Command {

    public MapInfoCMD(Minesumo minesumo) {
        super("map", "mapinfo");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl arena)) {
                player.sendMessage(Messages.GENERAL_NOT_IN_AN_ARENA.toTranslatable());
                return;
            }

            MapConfig config = arena.getConfig();
            int maxPlayer = config.getSpawnPositions().size() * config.getPlayerPerSpawnPosition();
            int minPlayer = config.getSpawnPositions().size();
            int maxLives = config.getStartingLives();

            sender.sendMessage(Messages.GAME_COMMAND_MAP_INFO.toTranslatable(
                    Component.text(config.getMapName()).color(NamedTextColor.GOLD),
                    Component.text(minPlayer).color(NamedTextColor.YELLOW),
                    Component.text(maxPlayer).color(NamedTextColor.YELLOW),
                    Component.text(maxLives).color(NamedTextColor.YELLOW)
            ));
        });


        MinecraftServer.getCommandManager().register(this);
    }
}
