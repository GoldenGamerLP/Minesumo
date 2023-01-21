package me.alex.minesumo.messages;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

public class HeadChat {


    private static final TextColor[][] defaultHead = new TextColor[][]{
            {color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25)},
            {color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25)},
            {color(25, 25, 25), color(25, 25, 25), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(12, 12, 12), color(25, 25, 25), color(25, 25, 25)},
            {color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(25, 25, 25), color(255, 215, 176), color(255, 215, 176), color(25, 25, 25)},

            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},

            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
            {color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176), color(255, 215, 176)},
    };

    private static final Cache<UUID, TextColor[][]> headCache = Caffeine.newBuilder()
            .softValues()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    @Blocking
    public static Component getHead(Player uid) {

        //TODO: Scoreboard heads per player per team

        TextColor[][] head = headCache.getIfPresent(uid.getUuid());
        //Optimize the image downloading
        if (head == null) {
            try {
                BufferedImage image = ImageIO.read(new URL("https://minotar.net/helm/" + uid.getUsername() + "/8.png"));
                head = new TextColor[image.getWidth()][image.getHeight()];
                for (int x = 0; x < image.getWidth(); x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        int rgb = image.getRGB(x, y);
                        head[x][y] = color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                    }
                }
                headCache.put(uid.getUuid(), head);
            } catch (Exception e) {
                e.printStackTrace();
                head = defaultHead;
            }
        }

        Component component = Component.empty();
        //Rotate the head clockwise
        for (int y = 0; y < head.length; y++) {
            for (int x = 0; x < head.length; x++) {
                component = component
                        .append(text((char) (((int) '\uF810') + y)).color(head[x][y]))
                        .append(text('\uE001'));
            }
            component = component.append(text('\uE008'));
        }
        return component;
    }

}
