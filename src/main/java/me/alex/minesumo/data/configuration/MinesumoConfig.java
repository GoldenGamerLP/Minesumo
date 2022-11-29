package me.alex.minesumo.data.configuration;

import de.leonhard.storage.Toml;
import me.hsgamer.hscore.config.annotated.AnnotatedConfig;
import me.hsgamer.hscore.config.annotation.ConfigPath;
import me.hsgamer.hscore.config.simplixstorage.LightningConfig;

import java.nio.file.Path;

public class MinesumoConfig extends AnnotatedConfig {

    @ConfigPath("spawn-schematic")
    String spawnSchematicName = "spawnschematic.schem";


    /**
     * Create an annotated config
     */
    public MinesumoConfig(Path path) {
        super(new LightningConfig<>(new Toml(path.resolve("config.toml").toFile())));
    }

}
