package me.alex.minesumo.data;

import dev.hypera.scaffolding.Scaffolding;
import lombok.extern.log4j.Log4j2;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import org.jglrxavpok.hephaistos.nbt.NBTException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Log4j2
public class SchematicLoader {

    private final MinesumoMainConfig config;
    private final Path schematicFolder;
    private final Predicate<MapConfig> mapConfigPredicate;

    private final CopyOnWriteArrayList<MapConfig> loadedMapConfigs;

    public SchematicLoader(Minesumo minesumo) {
        this.config = minesumo.getConfig();
        this.loadedMapConfigs = new CopyOnWriteArrayList<>();
        this.schematicFolder = minesumo.getDataDirectory().resolve(config.getSchematicFolder());
        this.schematicFolder.toFile().mkdirs();

        this.mapConfigPredicate = mapConfig -> {
            if (mapConfig == null) {
                log.warn("Map Configuration is null.");
                return true;
            }
            if (mapConfig.getMapName() == null) {
                log.warn("Map Name might not be null.");
                return true;
            }
            if (mapConfig.getGetSpawnPositions().length == 0) {
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

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> loadSchematics() {
        //Sort out wrong configs
        Set<MapConfig> currentConfigs = config.getMaps();
        currentConfigs.removeIf(this.mapConfigPredicate);

        //Parallel loading of maps
        CompletableFuture<Void>[] schematicLoading = new CompletableFuture[currentConfigs.size()];
        AtomicInteger index = new AtomicInteger(0);
        for (MapConfig config : currentConfigs) {

            schematicLoading[index.getAndIncrement()] = CompletableFuture
                    .completedFuture(config)
                    .thenApply(mapConfig -> schematicFolder.resolve(mapConfig.getSchematicFile()))
                    .thenCompose(path -> {
                        //Dumm code. Why throw an extra exception when we are using futures
                        try {
                            return Scaffolding.fromPath(path);
                        } catch (IOException | NBTException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .thenAccept(config::setMapSchematic)
                    .thenRun(() -> this.loadedMapConfigs.add(config));
        }

        return CompletableFuture.allOf(schematicLoading);
    }

    public List<MapConfig> getLoadedMapConfigs() {
        return List.copyOf(loadedMapConfigs);
    }
}
