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
import me.alex.minesumo.utils.json.JsonMapper;
import me.alex.minesumo.utils.json.configurations.JsonConfigurationLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.extensions.Extension;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Minesumo extends Extension {

    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private final long startTime = System.currentTimeMillis();
    private final MiniMessage miniMesssage = MiniMessage.miniMessage();
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
    private LuckPerms luckPerms;

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

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");

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

        this.getLuckperms();


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

        TabManager.defaultPrefix((player, teamBuilder) -> {
            if (luckPerms != null) {
                CachedMetaData data = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
                teamBuilder.withPrefix(miniMesssage.deserialize(data.getPrefix()));

            } else teamBuilder.withPrefix(Component.text(" "));

            //Black color
            teamBuilder.withColor(NamedTextColor.GRAY);

            long ranking = getStatsHandler().getPlayers().join() -
                    (getStatsHandler().getPlayerRanking(player.getUuid()).join() - 1);

            Component suffix = Component.text(" | ").color(NamedTextColor.GRAY)
                    .append(Component.text(ranking).color(NamedTextColor.YELLOW));

            teamBuilder.withSuffix(suffix);
        });

        MinecraftServer.getGlobalEventHandler().addChild(TabManager.getNode());

        hasStarted.set(true);
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

    public void getLuckperms() {
        //Test if class is available and try and catch

        this.luckPerms = LuckPermsProvider.get();
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
