package me.alex.minesumo.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.minestom.server.utils.url.URLUtils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class MojangUtils {
    private static final String FROM_UUID_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final String FROM_USERNAME_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final Cache<String, JsonObject> URL_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .softValues()
            .build();

    @Blocking
    public static @Nullable JsonObject fromUuid(@NotNull String uuid) {
        return retrieve(String.format(FROM_UUID_URL, uuid));
    }

    @Blocking
    public static @Nullable JsonObject fromUsername(@NotNull String username) {
        return retrieve(String.format(FROM_USERNAME_URL, username));
    }

    public static boolean isValidName(String name) {
        return name.matches("^[a-zA-Z0-9_]{3,16}$");
    }

    public static boolean isValidGameID(String gameID) {
        return gameID.matches("^(?=.*[a-z0-9])[a-z0-9]{6}$");
    }

    private static @Nullable JsonObject retrieve(@NotNull String url) {
        return URL_CACHE.get(url, s -> {
            try {
                // Retrieve from the rate-limited Mojang API
                final String response = URLUtils.getText(url);
                // If our response is "", that means the url did not get a proper object from the url
                // So the username or UUID was invalid, and therefore we return null
                if (response.isEmpty()) {
                    return null;
                }
                return JsonParser.parseString(response).getAsJsonObject();
            } catch (IOException e) {
                log.warn("Failed to retrieve data from Mojang API", e);
                throw new RuntimeException(e);
            }
        });
    }
}
