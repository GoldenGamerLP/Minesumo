package me.alex.minesumo.data.entities;

import lombok.Data;

import java.io.Serializable;

@Data
public final class MinesumoMainConfig implements Serializable {

    String schematicFolder = "schematics";

    Integer minPlayersToStart = 2,
            startingLives = 0,
            minLives = 0,
            maxLives = 3;

    Boolean isInEditorMode = false;

    Boolean fastJoin = false;

    String mongoDB = "mongoDB";

}
