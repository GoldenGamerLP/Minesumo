package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.hypera.scaffolding.schematic.Schematic;
import lombok.Data;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.List;

@Data
public class MapConfig {

    @Expose(deserialize = false, serialize = false)
    private Schematic mapSchematic = null;

    @SerializedName("mapName")
    @Expose
    private String mapName = "MyMap";

    @SerializedName("schematicFile")
    @Expose
    private String schematicFile = "default.schematic";

    @SerializedName("playerPerSpawnPosition")
    @Expose
    private Integer playerPerSpawnPosition = 1;

    @SerializedName("spawnPositions")
    @Expose
    private List<Pos> spawnPositions = new ArrayList<>();

    @SerializedName("deathLevel")
    @Expose
    private Double deathLevel = 0.0D;

    @SerializedName("startingLives")
    @Expose
    private Integer startingLives = 0;

    @SerializedName("spectatorPosition")
    @Expose
    private Pos spectatorPosition = Pos.ZERO;
}
