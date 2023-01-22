package me.alex.minesumo.utils.json;

import me.alex.minesumo.utils.json.provider.GsonProviderImpl;
import me.alex.minesumo.utils.json.provider.JacksonProviderImpl;
import me.alex.minesumo.utils.json.provider.MoshiProviderImpl;

public class JsonMapper {

    public static JsonProvider JSON_PROVIDER;

    public static void init(JsonProviders provider) {
        if (JSON_PROVIDER != null) {
            throw new IllegalStateException("JsonProvider is already initialized");
        }

        JSON_PROVIDER = provider.getProvider();
    }

    public enum JsonProviders {
        JACKSON(new JacksonProviderImpl()),
        MOSHI(new MoshiProviderImpl()),
        GSON(new GsonProviderImpl());

        private final JsonProvider provider;

        JsonProviders(JsonProvider provider) {
            this.provider = provider;
        }

        public JsonProvider getProvider() {
            return provider;
        }
    }
}
