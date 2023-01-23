package me.alex.minesumo.utils.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

public interface JsonProvider {

    //From InputStream and to OutputStream
    //To Json to file with writer
    //From Json from file with reader
    <T> void toJson(Class<T> clazz, T object, Writer writer);

    <T> String toJson(Class<T> clazz, T object);

    <T> void toJson(Class<T> clazz, T object, OutputStream outputStream);

    <T> T fromJson(String json, Class<T> clazz);

    <T> T fromJson(InputStream inputStream, Class<T> clazz);

    <T> T fromJson(Reader reader, Class<T> clazz);

    //add serializer and a deserializer
    <T> void addSerializer(Class<T> clazz, List<Object> serializers);
}
