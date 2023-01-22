package me.alex.minesumo.data.configuration.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.minestom.server.coordinate.Pos;

import java.io.IOException;

public class PosSerializer extends StdSerializer<Pos> {

    public PosSerializer() {
        super(Pos.class);
    }

    @Override
    public void serialize(Pos value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("x", value.x());
        gen.writeNumberField("y", value.y());
        gen.writeNumberField("z", value.z());
        gen.writeNumberField("yaw", value.yaw());
        gen.writeNumberField("pitch", value.pitch());
        gen.writeEndObject();
    }


}
