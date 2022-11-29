package me.alex.minesumo;

import me.alex.minesumo.data.configuration.MinesumoConfig;
import net.minestom.server.extensions.Extension;

public class Minesumo extends Extension {

    private MinesumoConfig cfg;

    @Override
    public void preInitialize() {
        cfg = new MinesumoConfig(this.getDataDirectory());
    }

    @Override
    public void initialize() {

    }

    @Override
    public void terminate() {

    }
}
