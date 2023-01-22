package me.alex.minesumo.data.configuration;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public class MinesumoMapConfig implements Serializable {

    Set<MapConfig> configurations = new HashSet<>();
}
