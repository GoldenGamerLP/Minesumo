package me.alex.minesumo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy HH:mm:ss");
        String formattedDateTime = now.format(formatter.localizedBy(Locale.FRENCH));
        System.out.println(formattedDateTime);
    }

}



