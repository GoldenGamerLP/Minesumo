package me.alex.minesumo.utils.json.provider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import me.alex.minesumo.utils.json.JsonProvider;

import java.io.*;
import java.util.List;

@Slf4j(topic = "Json-Provider")
public class JacksonProviderImpl implements JsonProvider {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public JacksonProviderImpl() {
        mapper.setVisibility(
                mapper.getSerializationConfig()
                        .getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        );
    }

    @Override
    public <T> String toJson(Class<T> clazz, T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void toJson(Class<T> clazz, T object, OutputStream outputStream) {
        try {
            mapper.writeValue(outputStream, object);
        } catch (Exception e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void toJson(Class<T> clazz, T object, Writer outputStream) {
        try {
            mapper.writeValue(outputStream, object);
        } catch (Exception e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        try {
            return mapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(Reader reader, Class<T> clazz) {
        try {
            return mapper.readValue(reader, clazz);
        } catch (IOException e) {
            log.error("Error while converting to json", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void addSerializer(Class<T> clazz, List<Object> serializers) {
        //Filter valid Serializer and deserializer for Jackson
        List<Object> validSerializers = serializers.stream()
                .filter(serializer -> serializer instanceof JsonSerializer || serializer instanceof JsonDeserializer)
                .toList();

        SimpleModule module = new SimpleModule();
        validSerializers.forEach(o -> {
            if (o instanceof JsonSerializer) {
                module.addSerializer((JsonSerializer<?>) o);
            } else if (o instanceof JsonDeserializer ser) {
                module.addDeserializer(clazz, (JsonDeserializer<T>) ser);
            }
        });

        mapper.registerModule(module);
    }
}
