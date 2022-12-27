package me.alex.minesumo;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.commands.ArenaCommand;
import me.alex.minesumo.commands.ArenaSetupCommand;
import me.alex.minesumo.data.MapCreator;
import me.alex.minesumo.data.SchematicLoader;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.listener.GlobalEventListener;
import me.alex.minesumo.manager.MapManager;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.extensions.Extension;

@Slf4j
public class Minesumo extends Extension {

    private JsonConfigurationLoader<MinesumoMainConfig> cfg;
    private SchematicLoader schematicLoader;
    private MapCreator mapCreator;
    private MapManager mapManager;

    @Override
    public void preInitialize() {
        //For Config Uses
        this.cfg = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("configuration.json").toFile(),
                MinesumoMainConfig.class
        );

        JsonConfigurationLoader.registerConverter(Pos.class, new PosConverter());
    }

    @Override
    public void initialize() {
        log.info("Initializing \n Loading configuration...");

        this.cfg.load();
        this.schematicLoader = new SchematicLoader(this);

        log.info("Loaded configuration! \n Loading schematics...");

        this.schematicLoader.loadSchematics().whenComplete((unused, throwable) -> {
            if (throwable != null) log.error("Error loading schematics", throwable);
            log.info("Loaded map configurations!");

            this.mapCreator = new MapCreator(this);
            this.mapManager = new MapManager(this);
            new GlobalEventListener(this);
        });

        new ArenaCommand("minesumo", this);
        new ArenaSetupCommand(this);
        log.info("Enabled GlobalEventHandler! \n >>> You can join now <<<");
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

    public MapCreator getMapCreator() {
        return mapCreator;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
