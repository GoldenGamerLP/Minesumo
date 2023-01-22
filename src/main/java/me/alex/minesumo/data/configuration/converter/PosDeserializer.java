package me.alex.minesumo.data.configuration.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import net.minestom.server.coordinate.Pos;

import java.io.IOException;

public class PosDeserializer extends StdDeserializer<Pos> {

    public PosDeserializer() {
        super(Pos.class);
    }

    @Override
    public Pos deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        return new Pos(
                node.get("x").asDouble(),
                node.get("y").asDouble(),
                node.get("z").asDouble(),
                node.get("yaw").floatValue(),
                node.get("pitch").floatValue()
        );
    }
}
