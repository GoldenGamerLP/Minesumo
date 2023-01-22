package me.alex.minesumo;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.commands.*;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.MinesumoMapConfig;
import me.alex.minesumo.data.configuration.converter.PosAdapter;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.data.configuration.converter.PosDeserializer;
import me.alex.minesumo.data.configuration.converter.PosSerializer;
import me.alex.minesumo.data.database.ArenaGameIDGenerator;
import me.alex.minesumo.data.database.MongoDB;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.data.statistics.StatisticFormatter;
import me.alex.minesumo.data.statistics.StatisticsManager;
import me.alex.minesumo.listener.GlobalEventListener;
import me.alex.minesumo.map.MapCreator;
import me.alex.minesumo.map.MapSelection;
import me.alex.minesumo.map.SchematicHandler;
import me.alex.minesumo.messages.MinesumoMessages;
import me.alex.minesumo.utils.json.JsonMapper;
import me.alex.minesumo.utils.json.configurations.JsonConfigurationLoader;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.extensions.Extension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Minesumo extends Extension {

    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private JsonConfigurationLoader<MinesumoMainConfig> mainCFG;
    private JsonConfigurationLoader<MinesumoMapConfig> mapCFG;
    private SchematicHandler schematicHandler;
    private MapCreator mapCreator;
    private MongoDB mongoDB;
    private StatisticDB statsHandler;
    private ArenaGameIDGenerator gameIDGenerator;
    private StatisticsManager statisticsManager;
    private StatisticFormatter statisticFormatter;
    private MapSelection mapSelection;

    private long startMS;

    @Override
    public void preInitialize() {
        startMS = System.currentTimeMillis();
        JsonMapper.init(JsonMapper.JsonProviders.MOSHI);

        JsonMapper.JSON_PROVIDER.addSerializer(Pos.class, List.of(
                new PosConverter(),
                new PosSerializer(),
                new PosAdapter(),
                new PosDeserializer())
        );

        //For Config Uses
        this.mainCFG = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("configuration.json").toFile(),
                MinesumoMainConfig.class
        );

        this.mapCFG = new JsonConfigurationLoader<>(
                this.getDataDirectory().resolve("maps.json").toFile(),
                MinesumoMapConfig.class
        );

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
        this.statisticsManager = new StatisticsManager(this);
        this.statisticFormatter = new StatisticFormatter(this);
        this.mapSelection = new MapSelection(this);

        new GlobalEventListener(this);

        if (getConfig().getIsInEditorMode()) {
            new ArenaSetupCMD(this);
            new ArenaCMD("debug", this);
        }

        new ForceLivesCMD(this);
        new ForceStartCMD(this);
        new GameIdCMD(this);
        new StatsCMD(this);

        log.info("Minesumo has been initialized!");
        log.info("Took {}ms", System.currentTimeMillis() - startMS);
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

    public MapSelection getMapSelection() {
        return mapSelection;
    }

    public MapCreator getMapCreator() {
        return mapCreator;
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
