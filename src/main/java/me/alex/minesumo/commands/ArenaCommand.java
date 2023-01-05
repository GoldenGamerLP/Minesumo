package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.instances.ArenaImpl;
import me.alex.minesumo.map.MapSelector;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ArenaCommand extends Command {

    public ArenaCommand(@NotNull String name, Minesumo minesumo) {
        super(name);

        var arg = ArgumentType.Literal("isArena");
        var playerArg = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        var change = ArgumentType.Literal("change");
        var changeEnum = ArgumentType.Enum("state", ArenaImpl.ArenaState.class);

        var selectMap = ArgumentType.Literal("selectMap");
        var maps = ArgumentType.Word("maps");

        var maxLifes = ArgumentType.Integer("maxLifes").max(3).min(0);

        var startGame = ArgumentType.Literal("start");

        maps.setSuggestionCallback((sender, context, suggestion) -> {
            minesumo.getSchematicLoader().getLoadedMapConfigs().forEach(mapConfig -> {
                suggestion.addEntry(new SuggestionEntry(mapConfig.getSchematicFile()));
            });
        });

        this.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl impl)) return;

            if (impl.getState() != ArenaImpl.ArenaState.WAITING_FOR_PLAYERS) return;
            if (impl.getPlayers(ArenaImpl.PlayerState.ALIVE).size() < 2) return;

            impl.changeArenaState(ArenaImpl.ArenaState.NORMAL_STARTING);
            player.sendMessage("Started");
        }, startGame);

        this.addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            if (!(player.getInstance() instanceof ArenaImpl impl)) return;

            if (impl.getState() != ArenaImpl.ArenaState.WAITING_FOR_PLAYERS) return;

            int maxLives = context.get(maxLifes);
            impl.setMaxLives(maxLives);
            player.sendMessage("Maxlifes: " + maxLives);
        }, maxLifes);

        this.addSyntax((sender, context) -> {
            String map = context.get(maps);
            Player p = (Player) sender;
            long ms = System.currentTimeMillis();
            p.sendMessage("Loading");
            Optional<MapConfig> cfg = minesumo.getMapManager().selectMap(mapConfig -> mapConfig.getSchematicFile().equals(map), MapSelector.MapSelectionStrategy.ANY_RESULT);
            p.sendMessage("Loading cfg" + (System.currentTimeMillis() - ms));
            cfg.ifPresentOrElse(mapConfig -> {
                p.sendMessage("Loaded mapConfig" + (System.currentTimeMillis() - ms));
                minesumo.getMapManager().getAvailableMap(mapConfig, ArenaImpl.ArenaState.WAITING_FOR_PLAYERS).thenAccept(arena -> {
                    p.sendMessage("Loading map" + (System.currentTimeMillis() - ms));
                    minesumo.getMapManager().queueArena(p, arena);
                    p.sendMessage("Done: " + (System.currentTimeMillis() - ms));
                });
            }, () -> sender.sendMessage("No map found"));
        }, selectMap, maps);

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
