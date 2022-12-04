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
    private Pos[] getSpawnPositions = new Pos[0];

    @SerializedName("deathLevel")
    @Expose
    private Double deathLevel = 0.0D;

    @SerializedName("spectatorPosition")
    @Expose
    private Pos spectatorPosition = Pos.ZERO;

    @Override
    public MapConfig clone() {
        try {
            MapConfig clone = (MapConfig) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
