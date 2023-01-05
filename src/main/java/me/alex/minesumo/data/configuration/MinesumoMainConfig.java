package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MinesumoMainConfig {


    @SerializedName("mapFolder")
    @Expose
    private final String schematicFolder = "schematics";


    @Expose
    private final Integer minPlayersToStart = 2,
            startingLives = 0,
            minLives = 0,
            maxLives = 3;


    @SerializedName("isEditorMode")
    @Expose
    private final Boolean isInEditorMode = false;

}
