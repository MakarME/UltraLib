package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils{
    public static String toLegacyHex(String message) {
        // Преобразует '&#RRGGBB' в '§x§R§R§G§G§B§B'
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            // создаём §x§R§R§G§G§B§B
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        // заменим обычные & кодики (&l, &r и т.д.)
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}