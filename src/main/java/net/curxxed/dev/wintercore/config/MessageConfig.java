package net.curxxed.dev.wintercore.config;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MessageConfig {

    private static final String FILE_NAME = "messages.yml";

    private final WinterCore plugin;
    private FileConfiguration config;

    public MessageConfig(WinterCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path, String fallback, String... placeholders) {
        String raw;
        if (config.isList(path)) {
            raw = String.join("\n", config.getStringList(path));
        } else {
            raw = config.getString(path, fallback);
        }
        return CC.translate(applyPlaceholders(raw, placeholders));
    }

    public List<String> getList(String path, List<String> fallback, String... placeholders) {
        List<String> raw = config.isList(path)
                ? config.getStringList(path)
                : new ArrayList<>(fallback);

        List<String> translated = new ArrayList<>();
        for (String line : raw) {
            translated.add(CC.translate(applyPlaceholders(line, placeholders)));
        }
        return translated;
    }

    public List<String> getList(String path, String... placeholders) {
        return getList(path, Collections.singletonList(""), placeholders);
    }

    private String applyPlaceholders(String input, String... placeholders) {
        String output = input == null ? "" : input;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return output;
    }
}
