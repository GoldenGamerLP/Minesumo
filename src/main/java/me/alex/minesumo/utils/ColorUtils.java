package me.alex.minesumo.utils;

import java.awt.*;

public class ColorUtils {
    private static final Color[] colorList = {Color.red, Color.cyan, Color.green, Color.magenta, Color.blue, Color.yellow, Color.black};
    private static int previousColor = -1;

    public static Color getComplimentaryColor(int seed) {
        int currentColor = (previousColor + colorList.length / 2) % colorList.length;
        previousColor = currentColor;
        return colorList[currentColor];
    }

    //Get a color from an int. This is used to get a color from a team id.
    public static Color getColorFromInt(int i) {
        return colorList[i % colorList.length];
    }
}
