package me.alex.minesumo.utils;

import java.text.DateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    //Format the duration between both dates. Use the format "HH:mm:ss" for the output. And put the letter behind the unit
    public static String formatTime(Date start, Date end) {
        Duration duration = Duration.between(start.toInstant(), end.toInstant());
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    //Format a date into the format which corresponds to the locale
    public static String formatDate(Date date, Locale locale) {
        return DateFormat.getDateInstance(DateFormat.LONG, locale).format(date);
    }
}
