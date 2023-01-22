package me.alex.minesumo.utils.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

public interface JsonProvider {

    <T> String toJson(Class<T> clazz, T object);

    <T> T fromJson(String json, Class<T> clazz);

    //From InputStream and to OutputStream
    <T> void toJson(Class<T> clazz, T object, OutputStream outputStream);

    <T> T fromJson(InputStream inputStream, Class<T> clazz);

    //To Json to file with writer
    //From Json from file with reader
    <T> void toJson(Class<T> clazz, T object, Writer writer);

    <T> T fromJson(Reader reader, Class<T> clazz);

    //add serializer and a deserializer
    <T> void addSerializer(Class<T> clazz, List<Object> serializers);
}
