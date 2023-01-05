package me.alex.minesumo.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

/**
 * Custom Class De-/Serializer. Can be used for extending classes. <p>
 *
 * <b>Loader Class Example:</b> JsonConfigurationLoader<MyConfig> cfg = new JsonConfigurationLoader(youreFile,MyConfig.class); <p>
 *
 * <b>Config Class:</b> Do not use a constructor with specifications and define variables with the annotation {@link SerializedName} & {@link Expose}. <p>
 * Use <b>Getters/Setters</b> to read/write to the class. <p>
 *
 * @param <T> The corresponding class
 * @author Alex W - GoldenGamer_LP
 */
@Slf4j(topic = "Configuration-Loader")
public final class JsonConfigurationLoader<T> {

    private static Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableInnerClassSerialization()
                .disableHtmlEscaping()
                .excludeFieldsWithoutExposeAnnotation()
                .create();


    }

    private final Class<T> of;
    private final File file;
    private T gsonObject;

    /**
     * Used for extending or creating new FileDataLoader class.
     *
     * @param file The File to write/read to.
     * @param of   The class to be used.
     */
    public JsonConfigurationLoader(@NotNull File file, @NotNull Class<T> of) {
        this.of = of;
        this.file = file;
    }

    public static void registerConverter(Class<?> clazz, Object obj) {
        gson = gson.newBuilder().registerTypeHierarchyAdapter(clazz, obj).create();
    }

    public JsonConfigurationLoader<T> load() {
        try {
            if (!file.exists()) {
                log.info("Creating [{}] file successfully: {}", this.file, createFileIfNotExists());
                gsonObject = of.getDeclaredConstructor().newInstance();
                writeToFile();
            } else readFromFile();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonConfigurationLoader<T> save() {
        writeToFile();
        return this;
    }

    public T getData() {
        return gsonObject;
    }

    @ApiStatus.Internal
    private Boolean createFileIfNotExists() {
        try {
            return file.mkdirs() && file.delete() && file.createNewFile();
        } catch (IOException e) {
            log.warn("Failed to create [{}]File with error: {}", this.file, e);
        }
        return false;
    }

    @ApiStatus.Internal
    private void writeToFile() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
            gson.toJson(gsonObject, bufferedWriter);
        } catch (IOException e) {
            log.warn("Failed to save [{}] File with error: {}", this.file, e);
        }
    }

    @ApiStatus.Internal
    private void readFromFile() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            gsonObject = gson.fromJson(bufferedReader, of);
        } catch (IOException e) {
            log.warn("Failed to save [{}] File with error: {}", this.file, e);
        }
    }
}
