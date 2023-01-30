package me.alex.minesumo.utils;

import java.awt.*;

public class ColorUtils {
    private static final Color[] colorList = {Color.red, Color.cyan, Color.green, Color.magenta, Color.blue, Color.yellow, Color.black, Color.white};
    private static int previousColor = -1;

    public static Color getComplimentaryColor(int seed) {
        int index = seed % colorList.length;
        int currentColor = (previousColor + colorList.length / 2) % colorList.length;
        previousColor = currentColor;
        return colorList[currentColor];
    }
}
