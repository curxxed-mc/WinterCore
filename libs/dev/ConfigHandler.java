package net.curxxed.dev;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class ConfigHandler {
    private final Main plugin;
    private File file;
    private FileConfiguration config;

    public ConfigHandler(Main plugin) {
        this.plugin = plugin;
        createConfig();
    }

    private void createConfig() {
        file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!file.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            e.printStackTrace();
            config = YamlConfiguration.loadConfiguration(file);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            e.printStackTrace();
            config = YamlConfiguration.loadConfiguration(file);
        }
    }
}