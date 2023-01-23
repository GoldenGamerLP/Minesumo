package me.alex.minesumo.utils.json.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.utils.json.JsonProvider;

import java.io.*;
import java.util.List;

@Slf4j(topic = "Json-Provider")
public class GsonProviderImpl implements JsonProvider {

    private Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .setLenient()
            .setPrettyPrinting()
            .create();

    @Override
    public <T> String toJson(Class<T> clazz, T object) {
        return gson.toJson(object);
    }

    @Override
    public <T> void toJson(Class<T> clazz, T object, OutputStream outputStream) {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            gson.toJson(object, writer);
        } catch (IOException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void toJson(Class<T> clazz, T object, Writer writer) {
        gson.toJson(object, writer);
    }

    @Override
    public <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(Reader reader, Class<T> clazz) {
        return gson.fromJson(reader, clazz);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    @Override
    public <T> void addSerializer(Class<T> clazz, List<Object> serializers) {
        List<Object> validSerializers = serializers.stream()
                .filter(serializer -> serializer instanceof JsonSerializer || serializer instanceof JsonDeserializer)
                .toList();

        if (validSerializers.isEmpty()) {
            return;
        }

        GsonBuilder builder = gson.newBuilder();
        //foreach serializer add it to the builder
        validSerializers.forEach(serializer -> builder.registerTypeAdapter(clazz, serializer));
        gson = builder.create();
    }
}
