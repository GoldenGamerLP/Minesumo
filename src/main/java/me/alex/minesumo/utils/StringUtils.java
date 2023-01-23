package me.alex.minesumo.utils;

import java.text.DecimalFormat;
import java.text.Format;

public class StringUtils {

    private static final Format DECIMAL_FORMAT = new DecimalFormat("###.##");

    //Get the first x letters of a string and append "..." if the string is longer than x
    public static String getFirstXLetters(String string, int x) {
        if (string.length() > x) {
            return string.substring(0, x) + "...";
        }
        return string;
    }

    public static String formatNumber(Number number) {
        return DECIMAL_FORMAT.format(number);
    }
}
