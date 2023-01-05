package me.alex.minesumo;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.commands.ArenaCommand;
import me.alex.minesumo.commands.ArenaSetupCommand;
import me.alex.minesumo.commands.ForceLives;
import me.alex.minesumo.commands.ForceStart;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.MinesumoMapConfig;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.listener.GlobalEventListener;
import me.alex.minesumo.listener.PvPEvents;
import me.alex.minesumo.map.MapCreator;
import me.alex.minesumo.map.MapSelector;
import me.alex.minesumo.map.SchematicHandler;
import me.alex.minesumo.messages.MinesumoMessages;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.extensions.Extension;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Minesumo extends Extension {

    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private JsonConfigurationLoader<MinesumoMainConfig> mainCFG;
    private JsonConfigurationLoader<MinesumoMapConfig> mapCFG;
    private SchematicHandler schematicHandler;
    private MapCreator mapCreator;
    private MapSelector mapSelector;

    @Override
    public void preInitialize() {
        //For Config Uses
        this.mainCFG = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("configuration.json").toFile(),
                MinesumoMainConfig.class
        );

        this.mapCFG = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("maps.json").toFile(),
                MinesumoMapConfig.class
        );

        JsonConfigurationLoader.registerConverter(Pos.class, new PosConverter());
        MinesumoMessages.innit();
    }

    @Override
    public void initialize() {
        log.info("Initializing \n Loading configuration...");

        this.mainCFG.load();
        this.mapCFG.load();
        this.schematicHandler = new SchematicHandler(this);

        log.info("Loaded configuration! \n Loading schematics...");

        this.schematicHandler.loadSchematics().whenComplete((unused, throwable) -> {
            if (throwable != null) log.error("Error loading schematics", throwable);
            log.info("Loaded map configurations!");

            this.mapCreator = new MapCreator(this);
            this.mapSelector = new MapSelector(this);

            hasStarted.set(true);
            log.info("Enabled GlobalEventHandler! \n You can join now");
        });

        new ArenaCommand("debug", this);
        new ArenaSetupCommand(this);
        new ForceLives(this);
        new ForceStart(this);
        new GlobalEventListener(this);

        PvPEvents arenaEvents = new PvPEvents(this);
    }

    @Override
    public void terminate() {

    }

    @Override
    public void preTerminate() {
        this.mainCFG.save();
        this.mainCFG = null;

        this.mapCFG.save();
        this.mapCFG = null;
    }

    public MinesumoMainConfig getConfig() {
        return mainCFG.getData();
    }

    public MinesumoMapConfig getMapConfig() {
        return mapCFG.getData();
    }

    public SchematicHandler getSchematicLoader() {
        return schematicHandler;
    }

    public MapCreator getMapCreator() {
        return mapCreator;
    }

    public MapSelector getMapManager() {
        return mapSelector;
    }

    public boolean hasStarted() {
        return hasStarted.get();
    }
}
