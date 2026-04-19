package net.curxxed.dev.wintercore.tags;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TagsManager {
    private final WinterCore plugin;
    private Map<String, Tag> tags = new HashMap<>();

    public TagsManager(WinterCore plugin) {
        this.plugin = plugin;
        loadTags();
    }

    public void loadTags() {
        File file = new File(plugin.getDataFolder(), "tags.yml");
        if (!file.exists()) {
            plugin.saveResource("tags.yml", false);
        }
        FileConfiguration config;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            config = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load tags.yml with UTF-8: " + e.getMessage());
            config = YamlConfiguration.loadConfiguration(file); // fallback
        }
        tags.clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;
            String prefix = section.getString("prefix", "");
            String color = section.getString("color", "WHITE");
            String name = section.getString("name", key);
            int weight = section.getInt("weight", 0);
            tags.put(key, new Tag(key, prefix, color, name, weight));
        }
    }

    public List<Tag> getTagsSorted() {
        return tags.values().stream()
                .sorted(Comparator.comparingInt(Tag::getWeight).reversed())
                .collect(Collectors.toList());
    }

    public Tag getTag(String id) {
        return tags.get(id);
    }

    public void setPlayerTag(UUID uuid, String tagId) {
        plugin.getDatabaseManager().getProfileService().setPlayerTag(uuid, tagId);
    }

    public void getPlayerTag(UUID uuid, Consumer<Tag> callback) {
        plugin.getDatabaseManager().getProfileService().getPlayerTag(uuid, tagId -> {
            Tag tag = tagId != null ? getTag(tagId) : null;
            callback.accept(tag);
        });
    }

    public static String colorNameToCode(String colorName) {
        if (colorName == null) return "&f";
        switch (colorName.toUpperCase()) {
            case "BLACK": return "&0";
            case "DARK_BLUE": return "&1";
            case "DARK_GREEN": return "&2";
            case "DARK_AQUA": return "&3";
            case "DARK_RED": return "&4";
            case "DARK_PURPLE": return "&5";
            case "GOLD": return "&6";
            case "GRAY": return "&7";
            case "DARK_GRAY": return "&8";
            case "BLUE": return "&9";
            case "GREEN": return "&a";
            case "AQUA": return "&b";
            case "RED": return "&c";
            case "LIGHT_PURPLE": return "&d";
            case "YELLOW": return "&e";
            case "WHITE": return "&f";
            default: return colorName.startsWith("&") ? colorName : "&f";
        }
    }
}

