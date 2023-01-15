package me.alex.minesumo.map;

import dev.hypera.scaffolding.Scaffolding;
import dev.hypera.scaffolding.schematic.Schematic;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.configuration.MinesumoMapConfig;
import me.alex.minesumo.instances.MinesumoInstance;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Slf4j
public class SchematicHandler {

    private final MinesumoMapConfig config;
    private final Path schematicFolder;
    private final Predicate<MapConfig> mapValidator;

    private final List<MapConfig> loadedMapConfigs;

    private final Minesumo minesumo;

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public SchematicHandler(Minesumo minesumo) {
        this.config = minesumo.getMapConfig();
        this.loadedMapConfigs = new CopyOnWriteArrayList<>();
        this.schematicFolder = minesumo.getDataDirectory().resolve("schematics");
        this.schematicFolder.toFile().mkdir();
        this.minesumo = minesumo;

        this.mapValidator = mapConfig -> {
            if (mapConfig == null) {
                log.warn("Map Configuration is null.");
                return true;
            }
            if (mapConfig.getMapName() == null) {
                log.warn("Map Name might not be null.");
                return true;
            }
            if (mapConfig.getSpawnPositions().size() == 0) {
                log.warn("Removing {} Map. Need more than two spawn positions!",
                        mapConfig.getMapName()
                );
                return true;
            }
            if (mapConfig.getPlayerPerSpawnPosition() == 0) {
                log.warn("Removing {} Map. Need more one player per team!",
                        mapConfig.getMapName()
                );
                return true;
            }
            if (!schematicFolder.resolve(mapConfig.getSchematicFile()).toFile().exists()) {
                log.warn("Removing {} Map. Schematic file {} was not found!",
                        mapConfig.getMapName(),
                        mapConfig.getSchematicFile()
                );
                return true;
            }
            return false;
        };
    }

    public CompletableFuture<MinesumoInstance> loadSchematic(MapConfig config) {
        CompletableFuture<Schematic> schematic;
        try {
            schematic = Scaffolding.fromPath(schematicFolder.resolve(config.getSchematicFile()));
        } catch (IOException | NBTException e) {
            return CompletableFuture.failedFuture(e);
        }

        return schematic.thenApply(schematic1 -> {
            if (schematic1 == null) return null;

            config.setMapSchematic(schematic1);
            return minesumo.getMapCreator().getEditorMap(config).join();
        });
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> loadSchematics() {
        //Sort out wrong configs
        Set<MapConfig> currentConfigs = config.getConfigurations();
        log.info("Found {} maps!", currentConfigs.size());
        currentConfigs.removeIf(this.mapValidator);
        log.info("After cleanup found {} maps!", currentConfigs.size());

        //Parallel loading of maps
        CompletableFuture<Void>[] schematicLoading = new CompletableFuture[currentConfigs.size()];
        AtomicInteger index = new AtomicInteger(0);
        for (MapConfig config : currentConfigs) {

            schematicLoading[index.getAndIncrement()] = CompletableFuture
                    .completedFuture(config)
                    .thenApply(mapConfig -> schematicFolder.resolve(mapConfig.getSchematicFile()))
                    .thenComposeAsync(path -> {
                        //TODO: Dumm code. Why throw an extra exception when we are using futures
                        try {
                            return Scaffolding.fromPath(path);
                        } catch (IOException | NBTException e) {
                            log.warn("Error while loading schematic: {}", path, e);
                            return CompletableFuture.failedFuture(e);
                        }
                    })
                    .thenAccept(config::setMapSchematic)
                    .thenRun(() -> {
                        log.info("Added schematic {}", config.getSchematicFile());
                        this.loadedMapConfigs.add(config);
                    });
        }

        return CompletableFuture.allOf(schematicLoading);
    }

    public List<MapConfig> getLoadedMapConfigs() {
        return List.copyOf(loadedMapConfigs);
    }

    public Path getSchematicFolder() {
        return schematicFolder;
    }
}
