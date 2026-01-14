package util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter DMYHM = DateTimeFormatter.ofPattern("dd.MM.yy - HH:mm");

    public static String formatDMYHM(long millis) {
        if (millis <= 0) return "Неверная дата";

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());

        return dateTime.format(DMYHM);
    }
}
