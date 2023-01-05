package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class MinesumoMapConfig {

    @Expose
    Set<MapConfig> configurations = new HashSet<>();
}
