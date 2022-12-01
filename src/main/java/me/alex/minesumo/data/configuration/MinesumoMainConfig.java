package me.alex.minesumo.data.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinesumoMainConfig {

    @Getter
    @SerializedName("mapFolder")
    @Expose
    private final String schematicFolder = "/schematicFolder";


    @Getter
    @SerializedName("mapList")
    @Expose
    private final Set<MapConfig> maps = new HashSet<>();

}
