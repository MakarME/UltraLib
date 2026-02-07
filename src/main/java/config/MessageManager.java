package config;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageManager {

    private static MessageManager instance;
    private FileConfiguration config;
    private File file;

    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }

    private MessageManager() {
        reload();
    }

    public void reload() {
        String fileName = "messages.yml";
        this.file = new File(SimplePlugin.getInstance().getDataFolder(), fileName);

        if (!file.exists()) {
            SimplePlugin.getInstance().saveResource(fileName, false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getString(Player player, String path, Map<String, String> placeholders) {
        if (!config.contains(path)) return ChatColor.RED + "Missing: " + path;

        if (config.isList(path)) {
            List<String> list = config.getStringList(path);
            String joined = String.join("\n", list);
            return format(player, joined, placeholders);
        }

        return format(player, config.getString(path), placeholders);
    }

    public String getString(Player player, String path) {
        return getString(player, path, new HashMap<>());
    }

    public String getString(Player player, String path, String... placeholders) {
        return getString(player, path, convertToMap(placeholders));
    }

    public List<String> getList(Player player, String path, String... placeholders) {
        return getList(player, path, convertToMap(placeholders));
    }

    public String getJoinedString(Player player, String path, String... placeholders) {
        return getJoinedString(player, path, convertToMap(placeholders));
    }

    private Map<String, String> convertToMap(String... placeholders) {
        Map<String, String> map = new HashMap<>();

        // Идем по массиву с шагом 2
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                map.put(placeholders[i], placeholders[i + 1]);
            }
        }
        return map;
    }

    public List<String> getList(Player player, String path, Map<String, String> placeholders) {
        if (!config.contains(path)) {
            return Collections.singletonList(ChatColor.RED + "Missing: " + path);
        }

        List<String> rawList;
        if (config.isList(path)) {
            // Это реально список
            rawList = config.getStringList(path);
        } else {
            // Это строка, но мы хотим список -> заворачиваем её
            rawList = Collections.singletonList(config.getString(path));
        }

        return format(player, rawList, placeholders);
    }

    public List<String> getList(Player player, String path) {
        return getList(player, path, new HashMap<>());
    }

    public String getJoinedString(Player player, String path, Map<String, String> placeholders) {
        List<String> lines = getList(player, path, placeholders);
        return String.join("\n", lines);
    }

    public String getJoinedString(Player player, String path) {
        return getJoinedString(player, path, new HashMap<>());
    }

    // --- Форматирование ---

    private String format(Player player, String message, Map<String, String> placeholders) {
        if (message == null) return "";

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) value = "";

            message = message.replace("{" + key + "}", value);
        }

        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        return Common.colorize(message);
    }

    private List<String> format(Player player, List<String> messages, Map<String, String> placeholders) {
        return messages.stream()
                .map(msg -> format(player, msg, placeholders))
                .collect(Collectors.toList());
    }
}