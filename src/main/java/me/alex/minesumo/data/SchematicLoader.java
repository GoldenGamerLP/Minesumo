package me.alex.minesumo.data;

import dev.hypera.scaffolding.Scaffolding;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import me.alex.minesumo.Minesumo;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Log4j2
public class SchematicLoader {

    private final MinesumoMainConfig config;
    private final Path schematicFolder;
    private final Predicate<MapConfig> mapConfigPredicate;

    public SchematicLoader(Minesumo minesumo) {
        this.config = minesumo.getConfig();
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

    @SneakyThrows
    public CompletableFuture<Void> loadSchematics() {
        //Sort out wrong configs
        List<MapConfig> currentConfigs = config.getMaps();
        currentConfigs.removeIf(this.mapConfigPredicate);

        //Parallel loading of maps
        CompletableFuture<Void>[] schematicLoading = new CompletableFuture[currentConfigs.size()];
        for (int i = currentConfigs.size() - 1; i >= 0; i--) {
            MapConfig config = currentConfigs.get(i);

            schematicLoading[i] = CompletableFuture
                    .completedFuture(config)
                    .thenApply(mapConfig -> schematicFolder.resolve(mapConfig.getSchematicFile()))
                    .thenCompose(Scaffolding::fromPath)
                    .thenAccept(config::setMapSchematic)
                    .exceptionally(throwable -> {
                        if (throwable != null) log.error("Error while reading schematic!", throwable);
                        return null;
                    });
        }

        return CompletableFuture.allOf(schematicLoading);
    }
}
