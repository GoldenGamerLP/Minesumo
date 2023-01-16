package me.alex.minesumo.utils;

import net.hollowcube.util.schem.Rotation;
import net.hollowcube.util.schem.Schematic;
import net.hollowcube.util.schem.SchematicReader;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class SchematicUtils {

    public static @NotNull Schematic ofPath(Path path) {
        try {
            return SchematicReader.read(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> pasteSchematic(Schematic schematic, Instance instance, Pos pos) {
        Point size = schematic.size();
        Point offset = schematic.offset();

        Point min = pos.add(offset);
        Point max = min.add(size);
        int chunkXStart = min.chunkX();
        int chunkXSize = max.chunkX() - min.chunkX() + 1;
        int chunkZStart = min.chunkZ();
        int chunkZSize = max.chunkZ() - min.chunkZ() + 1;
        ArrayList<CompletableFuture<Chunk>> chunksToLoad = new ArrayList<>();
        for (int i = chunkXStart; i < chunkXStart + chunkXSize; i++) {
            for (int j = chunkZStart; j < chunkZStart + chunkZSize; j++) {
                chunksToLoad.add(instance.loadOptionalChunk(i, j));
            }
        }

        return CompletableFuture.allOf(chunksToLoad.toArray(new CompletableFuture[0])).thenRun(() -> {
            schematic.build(Rotation.NONE, null).apply(instance, pos, null);
        });
    }
}
