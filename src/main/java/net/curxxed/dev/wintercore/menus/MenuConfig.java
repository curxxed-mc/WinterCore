package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
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
        return CC.translate(config.getString(path + ".title", "&6" + playerName + "'s History")
                .replace("{player}", playerName));
    }

    public int getSize(String path) {
        return config.getInt(path + ".size", 54);
    }

    public ItemStack buildItem(String path, String... placeholders) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return new ItemStack(Material.PAPER);

        Material mat = parseMaterial(section.getString("material", "PAPER"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = section.getString("name", "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            name = name.replace(placeholders[i], placeholders[i + 1]);
        }
        meta.setDisplayName(CC.translate(name));

        List<String> lore = section.getStringList("lore").stream()
                .map(CC::translate)
                .collect(Collectors.toList());
        if (!lore.isEmpty()) meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public int getSlot(String path) {
        return config.getInt(path + ".slot", 0);
    }

    public ConfigurationSection getSection(String path) {
        return config.getConfigurationSection(path);
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + name + "' in menus.yml, defaulting to PAPER.");
            return Material.PAPER;
        }
    }
}