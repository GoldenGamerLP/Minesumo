package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.hypera.scaffolding.schematic.Schematic;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minestom.server.coordinate.Pos;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
public class MapConfig implements Cloneable {

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

    @Override
    public MapConfig clone() {
        try {
            MapConfig clone = (MapConfig) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
