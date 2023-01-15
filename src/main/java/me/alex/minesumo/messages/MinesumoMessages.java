package me.alex.minesumo.messages;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minestom.server.adventure.MinestomAdventure;

import java.util.Locale;
import java.util.ResourceBundle;

public class MinesumoMessages {

    //Minesumo Prefix
    public static final Component PREFIX = Component.empty()
            .append(Component.text("Minesumo").color(TextColor.color(0xFF763B)))
            .append(Component.text(" | ").color(TextColor.color(0x434256)))
            .color(TextColor.color(0xD9E2E0));

    private static final String bundleName = "languages";
    private static final Locale[] locales = {
            new Locale("de", "DE"),
            new Locale("en", "US")
    };
    private static boolean isRegistered = false;

    public static void innit() {
        if (isRegistered) throw new RuntimeException("You cannot register messages twice.");

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;

        GlobalTranslator globalTranslator = GlobalTranslator.translator();
        TranslationRegistry registry = TranslationRegistry.create(Key.key("minsumo"));

        for (Locale locale : locales) {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
            registry.registerAll(locale, bundle, true);
        }

        globalTranslator.addSource(registry);
        isRegistered = true;
    }

}
