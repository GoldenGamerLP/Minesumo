package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.hypera.scaffolding.schematic.Schematic;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minestom.server.coordinate.Pos;

@Setter
@Getter
@RequiredArgsConstructor
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
    private Pos[] getSpawnPositions = new Pos[0];

    @SerializedName("deathLevel")
    @Expose
    private Pos deathPositions = Pos.ZERO;

    @SerializedName("spectatorPosition")
    @Expose
    private Pos spectatorPosition = Pos.ZERO;

}
