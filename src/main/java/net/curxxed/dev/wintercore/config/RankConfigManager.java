package net.curxxed.dev.wintercore.config;

import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class RankConfigManager {

    private final WinterCore plugin;
    @Getter
    private FileConfiguration ranksConfig;

    public RankConfigManager(WinterCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            this.ranksConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load ranks.yml: " + e.getMessage());
            this.ranksConfig = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void save() {
        try {
            ranksConfig.save(new File(plugin.getDataFolder(), "ranks.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save ranks.yml: " + e.getMessage());
        }
    }

    public ConfigurationSection getRanksSection() {
        return ranksConfig.getConfigurationSection("ranks");
    }

    public boolean rankExists(String rankName) {
        ConfigurationSection ranks = getRanksSection();
        return ranks != null && ranks.contains(rankName);
    }

    public void createRank(String rankName) {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null) ranks = ranksConfig.createSection("ranks");
        if (ranks.contains(rankName)) throw new IllegalArgumentException("rank already exists!");

        ConfigurationSection section = ranks.createSection(rankName);
        section.set("prefix", "");
        section.set("weight", getNextAvailableWeight(ranks));
        section.set("name-color", "&f");
        save();
    }

    public void deleteRank(String rankName) {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null || !ranks.contains(rankName)) throw new IllegalArgumentException("rank does not exist!");
        ranks.set(rankName, null);
        save();
    }

    public String getColor(String rank) {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null) return "&f";
        return ranks.getString(rank + ".name-color", "&f");
    }

    public String getPrefix(String rank) {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null) return "";
        ConfigurationSection section = ranks.getConfigurationSection(rank);
        return section != null ? CC.translate(section.getString("prefix", "")) : "";
    }

    public boolean isShowRankAboveHead() {
        return ranksConfig.getBoolean("show-rank-above-head", true);
    }

    public List<String> getAvailableRanks() {
        ConfigurationSection ranks = getRanksSection();
        return ranks != null ? new ArrayList<>(ranks.getKeys(false)) : new ArrayList<>();
    }

    public List<String> getSortedRanks() {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null) return new ArrayList<>();

        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        for (String rank : ranks.getKeys(false)) {
            entries.add(new AbstractMap.SimpleEntry<>(rank, ranks.getConfigurationSection(rank).getInt("weight", 0)));
        }
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            return cmp != 0 ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
        });

        List<String> sorted = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entries) sorted.add(e.getKey());
        return sorted;
    }

    public List<String> getPermissionsForRank(String rank) {
        List<String> permissions = new ArrayList<>();
        List<?> rawList = ranksConfig.getList("ranks." + rank + ".permissions", new ArrayList<>());
        for (Object obj : rawList) {
            if (obj instanceof String) {
                permissions.add((String) obj);
            } else if (obj instanceof Map) {
                Object perm = ((Map<?, ?>) obj).get("permission");
                if (perm instanceof String) permissions.add((String) perm);
            }
        }
        for (String parent : ranksConfig.getStringList("ranks." + rank + ".inheritance")) {
            permissions.addAll(getPermissionsForRank(parent));
        }
        return permissions;
    }

    private int getNextAvailableWeight(ConfigurationSection ranks) {
        int highest = 0;
        for (String rank : ranks.getKeys(false)) {
            int w = ranks.getConfigurationSection(rank).getInt("weight", 0);
            if (w > highest) highest = w;
        }
        return highest + 1;
    }
}