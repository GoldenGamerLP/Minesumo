package me.alex.minesumo;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.commands.*;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.MinesumoMapConfig;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.data.database.ArenaGameIDGenerator;
import me.alex.minesumo.data.database.MongoDB;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.data.statistics.StatisticFormatter;
import me.alex.minesumo.data.statistics.StatisticsManager;
import me.alex.minesumo.listener.GlobalEventListener;
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
    private MongoDB mongoDB;
    private StatisticDB statsHandler;
    private ArenaGameIDGenerator gameIDGenerator;
    private StatisticsManager statisticsManager;
    private StatisticFormatter statisticFormatter;

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
        log.info("Initializing configurations...");

        this.mainCFG.load();
        this.mapCFG.load();

        this.schematicHandler = new SchematicHandler(this);

        log.info("Loading Schematics...");
        this.schematicHandler.loadSchematics().join();
        log.info("Loaded schematics!");

        this.mongoDB = new MongoDB(this);
        this.statsHandler = new StatisticDB(this);
        this.gameIDGenerator = new ArenaGameIDGenerator(this);

        //Other Map stuff and so on
        this.mapCreator = new MapCreator(this);
        this.mapSelector = new MapSelector(this);
        this.statisticsManager = new StatisticsManager(this);
        this.statisticFormatter = new StatisticFormatter(this);

        new GlobalEventListener(this);

        if (getConfig().getIsInEditorMode())
            new ArenaCMD("debug", this);

        new ArenaSetupCMD(this);
        new ForceLivesCMD(this);
        new ForceStartCMD(this);
        new GameIdCMD(this);
        new StatsCMD(this);

        log.info("Minesumo has been initialized!");
        hasStarted.set(true);
    }

    @Override
    public void terminate() {
        this.mongoDB.close();
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

    public ArenaGameIDGenerator getGameIDGenerator() {
        return gameIDGenerator;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
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

    public MongoDB getMongoDB() {
        return mongoDB;
    }

    public boolean hasStarted() {
        return hasStarted.get();
    }

    public StatisticDB getStatsHandler() {
        return statsHandler;
    }

    public StatisticFormatter getStatisticFormatter() {
        return statisticFormatter;
    }
}
