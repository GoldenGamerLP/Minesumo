package me.alex.minesumo;

import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.commands.*;
import me.alex.minesumo.data.converter.GsonPosConverter;
import me.alex.minesumo.data.converter.JacksonPosDeserializer;
import me.alex.minesumo.data.converter.JacksonPosSerializer;
import me.alex.minesumo.data.converter.MoshiPosConverter;
import me.alex.minesumo.data.database.ArenaGameIDGenerator;
import me.alex.minesumo.data.database.MongoDB;
import me.alex.minesumo.data.database.StatisticDB;
import me.alex.minesumo.data.entities.MinesumoMainConfig;
import me.alex.minesumo.data.entities.MinesumoMapConfig;
import me.alex.minesumo.data.statistics.StatisticFormatter;
import me.alex.minesumo.data.statistics.StatisticsManager;
import me.alex.minesumo.listener.GlobalEventListener;
import me.alex.minesumo.map.MapCreator;
import me.alex.minesumo.map.MapSelection;
import me.alex.minesumo.map.SchematicHandler;
import me.alex.minesumo.messages.MinesumoMessages;
import me.alex.minesumo.tablist.TabManager;
import me.alex.minesumo.tablist.prefixprovider.DefaultPrefixProvider;
import me.alex.minesumo.tablist.prefixprovider.LuckpermsPrefixProvider;
import me.alex.minesumo.utils.json.JsonMapper;
import me.alex.minesumo.utils.json.configurations.JsonConfigurationLoader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extensions.ExtensionClassLoader;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Minesumo extends Extension {

    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private final long startTime = System.currentTimeMillis();
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

    @Override
    public void preInitialize() {
        //Important: If using other json libary -> Change build.gradle to implement the libary
        JsonMapper.init(JsonMapper.JsonProviders.GSON);

        //Add a new serializer for the Pos class
        JsonMapper.JSON_PROVIDER.addSerializer(Pos.class, List.of(
                new GsonPosConverter(),
                new JacksonPosSerializer(),
                new MoshiPosConverter(),
                new JacksonPosDeserializer())
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

        MinesumoMessages.init();
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
        new MapInfoCMD(this);

        //if LuckpermsProvder.class is available use LuckpermsPrefixProvider otherwise use DefaultPrefixProvider
        //This is a workaround because Luckperms is not a required dependency
        //If you want to use Luckperms as a required dependency you can remove the try and catch
        //we wait 1 seconds to make sure that Luckperms has been loaded
        MinecraftServer.getSchedulerManager().buildTask(() -> {
            Extension ex = MinecraftServer.getExtensionManager().getExtension("LuckPerms");
            if (ex != null) {
                //Log that Luckperms is available and we use LuckpermsPrefixProvider
                //FixMe: This is a workaround because Luckperms is not a required dependency
                log.info("Luckperms is available!");
                ExtensionClassLoader classLoader = (ExtensionClassLoader) ex.getClass().getClassLoader();
                ExtensionClassLoader thisClazz = (ExtensionClassLoader) this.getClass().getClassLoader();

                thisClazz.addChild(classLoader);
                TabManager.defaultPrefix(new LuckpermsPrefixProvider(this.getStatsHandler()));
            } else {
                //Log that Luckperms is not available and we use DefaultPrefixProvider
                log.info("Using default prefix provider!");
                TabManager.defaultPrefix(new DefaultPrefixProvider(this.getStatsHandler()));
            }
            this.getEventNode().addChild(TabManager.getNode());
            hasStarted.set(true);
        }).delay(Duration.ofMillis(500)).schedule();

        log.info("Minesumo has been initialized!");
        log.info("Took {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void terminate() {
        this.mongoDB.close();
    }

    @Override
    public void preTerminate() {
        //only save configs if editor mode is enabled
        if (getConfig().getIsInEditorMode()) {
            this.mainCFG.save();
            this.mapCFG.save();
        }
        this.mainCFG = null;
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
