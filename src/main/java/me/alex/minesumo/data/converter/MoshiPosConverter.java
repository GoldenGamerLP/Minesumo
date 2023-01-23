package me.alex.minesumo.data.converter;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;
import me.alex.minesumo.utils.json.Moshi;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

public class MoshiPosConverter implements Moshi {

    @ToJson
    public String[] toJson(Pos pos) {
        return new String[]{
                String.valueOf(pos.x()),
                String.valueOf(pos.y()),
                String.valueOf(pos.z()),
                String.valueOf(pos.yaw()),
                String.valueOf(pos.pitch())
        };
    }

    @FromJson
    public Pos fromJson(@Nullable String[] pos) {
        if (pos == null || pos.length != 5) {
            return Pos.ZERO;
        }
        return new Pos(
                Double.parseDouble(pos[0]),
                Double.parseDouble(pos[1]),
                Double.parseDouble(pos[2]),
                Float.parseFloat(pos[3]),
                Float.parseFloat(pos[4])
        );
    }
}
