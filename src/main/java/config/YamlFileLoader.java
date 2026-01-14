package config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class YamlFileLoader {

    private static final Map<String, FileConfiguration> configs = new HashMap<>();

    public YamlFileLoader(String fileName) {
        File file = new File(SimplePlugin.getInstance().getDataFolder(), fileName);

        if (!file.exists()) {
            SimplePlugin.getInstance().saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = SimplePlugin.getInstance().getResource(fileName);
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
        }

        configs.put(fileName, config);
    }

    public static FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }
}
