package me.alex.minesumo.data.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import lombok.Data;
import net.hollowcube.util.schem.Schematic;
import net.minestom.server.coordinate.Pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class MapConfig implements Serializable {

    @JsonIgnore
    @Expose(serialize = false, deserialize = false)
    transient Schematic mapSchematic = null;

    String mapName = "MyMap";

    String schematicFile = "default.schematic";

    Integer playerPerSpawnPosition = 1;

    List<Pos> spawnPositions = new ArrayList<>();

    Double deathLevel = 0.0D;

    Integer startingLives = 0;

    Pos spectatorPosition = Pos.ZERO;
}
