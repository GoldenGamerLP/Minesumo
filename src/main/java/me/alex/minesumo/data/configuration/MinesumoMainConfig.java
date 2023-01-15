package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MinesumoMainConfig {


    @SerializedName("mapFolder")
    @Expose
    private String schematicFolder = "schematics";


    @Expose
    private Integer minPlayersToStart = 2,
            startingLives = 0,
            minLives = 0,
            maxLives = 3;


    @SerializedName("isEditorMode")
    @Expose
    private Boolean isInEditorMode = false;

    @SerializedName("fastJoin")
    @Expose
    private Boolean fastJoin = false;

    @SerializedName("mongoDB")
    @Expose
    private String mongoDB = "mongoDB";

}
