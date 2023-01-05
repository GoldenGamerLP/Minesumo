package me.alex.minesumo.commands;

import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.instances.MinesumoInstance;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class ArenaSetupCommand extends Command {

    private final Map<UUID, MinesumoInstance> activeMaps;

    public ArenaSetupCommand(Minesumo minesumo) {
        super("setup");
        this.activeMaps = new HashMap<>();

        this.setCondition(Conditions::playerOnly);

        var nameArgument = ArgumentType.Word("nameArgument");

        var join = ArgumentType.Literal("join");

        var set = ArgumentType.Literal("set");

        var remove = ArgumentType.Literal("remove");

        var add = ArgumentType.Literal("add");

        var spawn = ArgumentType.Literal("spawn");

        var spectator = ArgumentType.Literal("spectator");

        var deathHeight = ArgumentType.Literal("deathHeight");

        var spawnID = ArgumentType.Integer("spawnID");

        var save = ArgumentType.Literal("SAVE");

        nameArgument.setSuggestionCallback((sender, context, suggestion) -> {
            try (Stream<Path> paths = Files.walk(minesumo.getSchematicLoader().getSchematicFolder())) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String name = path.getFileName().toString();
                    suggestion.addEntry(new SuggestionEntry(name));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        addSyntax((sender, context) -> {
            sender.sendMessage("WADASSADMLMKFMKOFMFKMKFMFMK");
            if (!(sender instanceof Player player)) return;
            if (this.activeMaps.containsKey(player.getUuid())) {
                player.sendMessage("You are already setupping.");
                return;
            }

            Path schematic = minesumo.getSchematicLoader().getSchematicFolder()
                    .resolve(context.get(nameArgument));

            if (!Files.exists(schematic)) {
                player.sendMessage("The schematic does not exist");
                return;
            }

            MapConfig mapConfig = new MapConfig();
            mapConfig.setSchematicFile(schematic.toFile().getName());

            minesumo.getSchematicLoader().loadSchematic(mapConfig).whenCompleteAsync((minesumoInstance, throwable) -> {
                if (throwable != null) {
                    player.sendMessage(throwable.getLocalizedMessage());
                    return;
                }

                if (minesumoInstance == null) {
                    player.sendMessage("That map was not valid.");
                    return;
                }

                player.setInstance(minesumoInstance, Pos.ZERO.add(0, 100, 0)).thenRun(() -> {
                    this.activeMaps.put(player.getUuid(), minesumoInstance);
                    minesumoInstance.eventNode().addListener(RemoveEntityFromInstanceEvent.class, removeEntityFromInstanceEvent -> {
                        if (!(removeEntityFromInstanceEvent.getEntity() instanceof Player pe)) return;
                        if (!this.activeMaps.containsKey(pe.getUuid())) return;

                        MinecraftServer.getInstanceManager().unregisterInstance(this.activeMaps.remove(pe.getUuid()));
                    });
                    player.sendMessage("You are now setupping.");
                });
            });
        }, join, nameArgument);

        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            if (!this.activeMaps.containsKey(player.getUuid())) {
                player.sendMessage("You are not setupping.");
                return;
            }

            this.activeMaps.get(player.getUuid()).getConfig().setSpectatorPosition(player.getPosition());
            player.sendMessage("Set spec spawn!");
        }, spectator, set);

        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            if (!this.activeMaps.containsKey(player.getUuid())) {
                player.sendMessage("You are not setupping.");
                return;
            }

            Integer yLevel = context.get("spawnID");
            if (yLevel == null) {
                player.sendMessage("The y level cannot be null.");
                return;
            }

            MapConfig mapConfig = this.activeMaps.get(player.getUuid()).getConfig();

            if (mapConfig.getSpawnPositions().isEmpty()) {
                player.sendMessage("Set spawn positions before setting death level");
                return;
            }

            boolean wrongYLevel = mapConfig.getSpawnPositions().stream().anyMatch(pos -> pos.y() < yLevel);

            if (wrongYLevel) {
                player.sendMessage("The death level must be lower than the spawn positions.");
                return;
            }

            mapConfig.setDeathLevel(Double.valueOf(yLevel));
            player.sendMessage("Set spec spawn!");
        }, deathHeight, set, spawnID);

        addSyntax(this::addSpawn, spawn, add);

        addSyntax(this::removeSpawn, spawn, remove, spawnID);

        addSyntax((sender, context) -> {
            Player player = (Player) sender;
            if (!this.activeMaps.containsKey(player.getUuid())) {
                player.sendMessage("You are not setupping");
                return;
            }

            MapConfig config = this.activeMaps.remove(player.getUuid()).getConfig();
            minesumo.getMapConfig().getConfigurations().add(config);
            player.sendMessage("Please restart to ensure using the config correctly. \n You can settup another arena.");
            MinecraftServer.stopCleanly();
        }, save);

        MinecraftServer.getCommandManager().register(this);
    }

    private void removeSpawn(CommandSender sender, CommandContext commandContext) {
        Player player = (Player) sender;

        if (!this.activeMaps.containsKey(player.getUuid())) {
            player.sendMessage("You are not setupping.");
            return;
        }

        Integer index = commandContext.get("spawnID");
        if (index == null) {
            player.sendMessage("No spawnID or invalid SpawnID");
            return;
        }

        MapConfig mapConfig = this.activeMaps.get(player.getUuid()).getConfig();
        if (mapConfig.getSpawnPositions().get(index) == null) {
            player.sendMessage("The spawn position at " + index + " is not available.");
            return;
        }

        Pos before = mapConfig.getSpawnPositions().remove(index.intValue());
        player.sendMessage("Removed " + before + " with ID: " + index);
    }

    private void addSpawn(CommandSender sender, CommandContext commandContext) {
        Player player = (Player) sender;

        if (!this.activeMaps.containsKey(player.getUuid())) {
            player.sendMessage("You are not setupping.");
            return;
        }

        MapConfig mapConfig = this.activeMaps.get(player.getUuid()).getConfig();

        Pos pos = player.getPosition();
        boolean isAdded = mapConfig.getSpawnPositions().add(pos);
        int index = mapConfig.getSpawnPositions().indexOf(pos);
        player.sendMessage("Added a spawn at " + player.getPosition() + " with ID: " + index);

    }
}
