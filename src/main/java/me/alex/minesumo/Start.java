package me.alex.minesumo;

import me.alex.minesumo.data.configuration.MapConfig;
import me.alex.minesumo.data.configuration.MinesumoMainConfig;
import me.alex.minesumo.data.configuration.converter.PosConverter;
import me.alex.minesumo.utils.JsonConfigurationLoader;
import net.minestom.server.coordinate.Pos;

import java.io.File;

public class Start {

    public static void main(String[] args) {
        File file = new File(System.getProperty("user.dir") + "/config.json");
        JsonConfigurationLoader<MinesumoMainConfig> cfg =
                new JsonConfigurationLoader<>(file, MinesumoMainConfig.class);

        JsonConfigurationLoader.registerConverter(Pos.class, new PosConverter());

        MinesumoMainConfig config = cfg.load().getData();

        config.getMaps().add(new MapConfig());

        cfg.save();

    }
}
