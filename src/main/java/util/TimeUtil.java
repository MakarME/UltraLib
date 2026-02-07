package util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

    private static final DateTimeFormatter DMYHM = DateTimeFormatter.ofPattern("dd.MM.yy - HH:mm");

    public static String formatDMYHM(long millis) {
        if (millis <= 0) return "Неверная дата";

        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());

        return dateTime.format(DMYHM);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(SERVER_ZONE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(SERVER_ZONE);

    /**
     * Конвертирует миллисекунды в строку времени (например, "14:30:05")
     * по локальному времени сервера.
     */
    public static String formatTime(long millis) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    /**
     * Конвертирует миллисекунды в строку даты (например, "30.01.2025")
     * по локальному времени сервера.
     */
    public static String formatDate(long millis) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "0";

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if(minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%02d", seconds);
        }
    }
}
