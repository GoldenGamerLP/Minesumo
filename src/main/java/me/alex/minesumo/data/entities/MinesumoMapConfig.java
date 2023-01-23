package me.alex.minesumo.data.entities;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Data
public final class MinesumoMapConfig implements Serializable {

    final Set<MapConfig> configurations = new HashSet<>();
}
