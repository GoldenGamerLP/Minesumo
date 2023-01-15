package me.alex.minesumo.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

public class HeadChat {


    public static final TextColor[][] defaultHead = new TextColor[][]{
            {color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25)},
            {color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25)},
            {color(25, 25, 25), color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25), color(25, 25, 25)},
            {color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(255, 215, 176), color(255, 215, 176), color(25, 25, 25)},

            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},

            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
    };

    public static final HashMap<UUID, TextColor[][]> headCache = new HashMap<>();

    public static Component getHead(Player uid) {

        TextColor[][] head = headCache.computeIfAbsent(uid.getUuid(), uuid -> {
            PlayerSkin playerSkin = PlayerSkin.fromUsername(uid.getUsername());
            PlayerSkin skin = uid.getSkin();

            if (playerSkin == null) {
                System.out.println("no");
                return defaultHead;
            }

            String json = new String(Base64.getDecoder().decode(playerSkin.textures()));
            String textureUrl = json.split("\"SKIN\"")[1].split("\"")[3];

            BufferedImage image;
            try {
                image = ImageIO.read(new URL(textureUrl));
            } catch (Exception e) {
                System.out.println("Failed to get image.");
                e.printStackTrace();
                return defaultHead;
            }

            TextColor[][] playerHead = new TextColor[8][8];
            for (int x = 8; x < 16; x++) {
                for (int y = 8; y < 16; y++) {
                    int rgb = image.getRGB(x, y);
                    playerHead[y - 8][x - 8] = color(rgb);
                }
            }

            return playerHead;
        });

        Component component = Component.empty();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                component = component
                        .append(text((char) (((int) '\uF810') + y)).color(head[y][x]))
                        .append(text('\uE001'));
            }
            component = component.append(text('\uE008'));
        }
        return component;
    }

}
