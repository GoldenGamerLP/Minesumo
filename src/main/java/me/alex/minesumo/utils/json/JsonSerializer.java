package me.alex.minesumo.utils.json;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public abstract class JsonSerializer<T> extends StdSerializer<T> implements com.google.gson.JsonSerializer<T> {

    protected JsonSerializer(Class<T> t) {
        super(t);
    }
}
