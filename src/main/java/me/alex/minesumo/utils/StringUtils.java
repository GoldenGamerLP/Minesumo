package me.alex.minesumo.utils;

public class StringUtils {

    //Get the first x letters of a string and append "..." if the string is longer than x
    public static String getFirstXLetters(String string, int x) {
        if (string.length() > x) {
            return string.substring(0, x) + "...";
        }
        return string;
    }
}
