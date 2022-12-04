package me.alex.minesumo;

import lombok.extern.log4j.Log4j2;
import me.alex.minesumo.data.SchematicLoader;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.listener.GlobalEventListener;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import net.minestom.server.extensions.Extension;

@Log4j2
public class Minesumo extends Extension {

    private JsonConfigurationLoader<MinesumoMainConfig> cfg;
    private SchematicLoader schematicLoader;

    @Override
    public void preInitialize() {
        //For Config Uses
        this.cfg = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("/configuration.json").toFile(),
                MinesumoMainConfig.class
        );

        this.schematicLoader = new SchematicLoader(this);
    }

    @Override
    public void initialize() {
        log.info("Initializing \n Loading configuration...");

        this.cfg.load();

        log.info("Loaded configuration! \n Loading schematics...");

        this.schematicLoader.loadSchematics().whenComplete((unused, throwable) -> {
            if (throwable != null) log.error("Error loading schematics", throwable);
            log.info("Loaded map configurations!");
        });

        log.info("Enabling GlobalEventHandler");
        new GlobalEventListener();
    }

    @Override
    public void terminate() {

    }

    @Override
    public void preTerminate() {
        this.cfg.save();
        this.cfg = null;
    }

    public MinesumoMainConfig getConfig() {
        return cfg.getData();
    }

    public SchematicLoader getSchematicLoader() {
        return schematicLoader;
    }
}
