package me.alex.minesumo;

import lombok.extern.java.Log;
import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import net.minestom.server.coordinate.Pos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Log
public class Start {

    public static void main(String[] args) {
        Logger logger = LogManager.getLogger("Start");
        logger.info("Starting");
        File file = new File(System.getProperty("user.dir") + "/config.json");
        JsonConfigurationLoader<MinesumoMainConfig> cfg =
                new JsonConfigurationLoader<>(file, MinesumoMainConfig.class);


        JsonConfigurationLoader.registerConverter(Pos.class, new PosConverter());

        MinesumoMainConfig config = cfg.load().getData();

        config.getMaps().add(new MapConfig());

        cfg.save();

    }
}
