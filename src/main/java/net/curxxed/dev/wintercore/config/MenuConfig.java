package net.curxxed.dev.wintercore.config;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MenuConfig {

    private final WinterCore plugin;
    private FileConfiguration config;

    public MenuConfig(WinterCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "menus.yml");
        if (!file.exists()) plugin.saveResource("menus.yml", false);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(new File(plugin.getDataFolder(), "menus.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save menus.yml: " + e.getMessage());
        }
    }

    public String getTitle(String path, String playerName) {
        return getString(path + ".title", "&6" + playerName + "'s History", "{player}", playerName);
    }

    public int getSize(String path) {
        return getInventorySize(path + ".size", 54);
    }

    public int getInventorySize(String path, int fallback) {
        int configured = config.getInt(path, fallback);
        if (configured <= 0) {
            configured = fallback;
        }
        configured = Math.max(9, Math.min(54, configured));
        return ((configured - 1) / 9 + 1) * 9;
    }

    public int getInt(String path, int fallback) {
        return config.getInt(path, fallback);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return config.getBoolean(path, fallback);
    }

    public String getString(String path, String fallback, String... placeholders) {
        return CC.translate(applyPlaceholders(config.getString(path, fallback), placeholders));
    }

    public List<String> getStringList(String path, List<String> fallback, String... placeholders) {
        List<String> lines = config.isList(path)
                ? config.getStringList(path)
                : new ArrayList<>(fallback);

        return lines.stream()
                .map(line -> CC.translate(applyPlaceholders(line, placeholders)))
                .collect(Collectors.toList());
    }

    public ItemStack buildItem(String path, String... placeholders) {
        return buildItem(path, Material.PAPER, (short) 0, placeholders);
    }

    public ItemStack buildItem(String path, Material fallbackMaterial, String... placeholders) {
        return buildItem(path, fallbackMaterial, (short) 0, placeholders);
    }

    public ItemStack buildItem(String path, Material fallbackMaterial, short fallbackData, String... placeholders) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            plugin.getLogger().warning("[MenuConfig] Missing config section '" + path
                    + "' in menus.yml — returning fallback item. Check your menus.yml.");
            return new ItemBuilder(fallbackMaterial, 1, fallbackData).toItemStack();
        }

        Material mat = parseMaterial(section.getString("material", fallbackMaterial.name()), fallbackMaterial);
        int amount = Math.max(1, Math.min(64, section.getInt("amount", 1)));
        short data = (short) section.getInt("data", fallbackData);
        ItemBuilder itemBuilder = new ItemBuilder(mat, amount, data);
        ItemMeta meta = itemBuilder.toItemStack().getItemMeta();
        if (meta == null) return itemBuilder.toItemStack();

        String name = section.getString("name", "");
        if (!name.isEmpty()) {
            itemBuilder.setName(applyPlaceholders(name, placeholders));
        }

        List<String> lore = section.getStringList("lore").stream()
                .map(line -> applyPlaceholders(line, placeholders))
                .collect(Collectors.toList());
        if (!lore.isEmpty()) itemBuilder.setLore(lore);

        return itemBuilder.toItemStack();
    }

    public int getSlot(String path) {
        return config.getInt(path + ".slot", 0);
    }

    public int getSlot(String path, int fallback) {
        return config.getInt(path + ".slot", fallback);
    }

    public List<String> getKeys(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    private Material parseMaterial(String name) {
        return parseMaterial(name, Material.PAPER);
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + name + "' in menus.yml, defaulting to " + fallback.name() + ".");
            return fallback;
        }
    }

    private String applyPlaceholders(String input, String... placeholders) {
        String output = input == null ? "" : input;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return output;
    }
}
