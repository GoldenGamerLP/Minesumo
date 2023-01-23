package me.alex.minesumo.data.converter;

import com.google.gson.*;
import net.minestom.server.coordinate.Pos;

import java.lang.reflect.Type;

public class GsonPosConverter implements JsonSerializer<Pos>, JsonDeserializer<Pos> {

    @Override
    public Pos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject js = json.getAsJsonObject();
        return new Pos(
                js.get("x").getAsDouble(),
                js.get("y").getAsDouble(),
                js.get("z").getAsDouble(),
                js.get("yaw").getAsFloat(),
                js.get("pitch").getAsFloat()
        );
    }

    @Override
    public JsonElement serialize(Pos src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("x", src.x());
        jsonObject.addProperty("y", src.y());
        jsonObject.addProperty("z", src.z());
        jsonObject.addProperty("pitch", src.pitch());
        jsonObject.addProperty("yaw", src.yaw());
        return jsonObject;
    }
}
