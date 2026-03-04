package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryMenu implements Listener {

    private final WinterCore plugin;
    private final DatabaseManager db;
    private final MenuConfig menuConfig;
    private final Map<UUID, HistroyMenuContext> contexts = new ConcurrentHashMap<>();

    public HistoryMenu(WinterCore plugin, MenuConfig menuConfig) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.menuConfig = menuConfig;
    }

    public void open(Player viewer, String targetName, UUID targetUuid) {
        contexts.put(viewer.getUniqueId(), new HistroyMenuContext(targetName, targetUuid));
        String title = menuConfig.getTitle("history-menu", targetName);
        int size = menuConfig.getSize("history-menu");
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection items = menuConfig.getSection("history-menu.items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                int slot = menuConfig.getSlot("history-menu.items." + key);
                inv.setItem(slot, menuConfig.buildItem("history-menu.items." + key));
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> viewer.openInventory(inv));
    }

    private void openCategory(Player viewer, String category) {
        HistroyMenuContext context = contexts.get(viewer.getUniqueId());
        if (context == null) return;

        context.setCurrentCategory(category);
        String targetName = context.getPlayerName();
        UUID targetUuid = context.getUuid();

        switch (category.toLowerCase()) {
            case "warnings":
                db.getWarnings(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.warning-item",
                                "&7Reason: " + map.getOrDefault("reason", "Unknown"),
                                "&7Issuer: " + map.getOrDefault("issuer", "Unknown"),
                                "&7Date: " + map.getOrDefault("date", "Unknown")));
                    }
                    openPaginated(viewer, items, menuConfig.getTitle("history-menu", targetName)
                            .replace(targetName + "'s History", targetName + "'s Warnings"));
                });
                break;
            case "mutes":
                db.getMutes(targetUuid, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.mute-item",
                                "&7Reason: " + map.getOrDefault("reason", "Unknown"),
                                "&7Issuer: " + map.getOrDefault("issuer", "Unknown"),
                                "&7Expires: " + map.getOrDefault("expiration", "Unknown")));
                    }
                    openPaginated(viewer, items, CC.translate("&6" + targetName + "'s Mutes"));
                });
                break;
            case "bans":
                db.getBans(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.ban-item",
                                "&7Reason: " + map.getOrDefault("reason", "Unknown"),
                                "&7Issuer: " + map.getOrDefault("issuer", "Unknown"),
                                "&7Date: " + map.getOrDefault("date", "Unknown"),
                                "&7Expires: " + map.getOrDefault("expiration", "Permanent")));
                    }
                    openPaginated(viewer, items, CC.translate("&6" + targetName + "'s Bans"));
                });
                break;
            case "grants":
                db.getRankGrants(targetUuid, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.grant-item",
                                "&7Rank: " + map.getOrDefault("rank", "Unknown"),
                                "&7Granted by: " + map.getOrDefault("issuer", "Unknown"),
                                "&7Date: " + map.getOrDefault("date", "Unknown"),
                                "&7Expires: " + map.getOrDefault("expiration", "Permanent"),
                                "&7Reason: " + map.getOrDefault("reason", "Unknown")));
                    }
                    openPaginated(viewer, items, CC.translate("&6" + targetName + "'s Grants"));
                });
                break;
        }
    }

    private void openPaginated(Player viewer, List<ItemStack> items, String title) {
        int size = menuConfig.getSize("history-category");
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            inv.setItem(i, items.get(i));
        }

        int backSlot = menuConfig.getSlot("history-category.back-button");
        inv.setItem(backSlot, menuConfig.buildItem("history-category.back-button"));

        Bukkit.getScheduler().runTask(plugin, () -> viewer.openInventory(inv));
    }

    private ItemStack buildEntryItem(String configPath, String... loreLines) {
        ItemStack base = menuConfig.buildItem(configPath);
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        List<String> lore = new ArrayList<>();
        for (String line : loreLines) lore.add(CC.translate(line));
        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = Utilities.getInventoryTitle(event);
        if (title == null) return;

        HistroyMenuContext context = contexts.get(player.getUniqueId());
        if (context == null) return;

        boolean isOurMenu = title.contains("'s History") || title.contains("'s Warnings")
                || title.contains("'s Mutes") || title.contains("'s Bans") || title.contains("'s Grants");
        if (!isOurMenu) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase();

        if (clicked.getType() == Material.ARROW || displayName.contains("back")) {
            open(player, context.getPlayerName(), context.getUuid());
            return;
        }

        if (title.endsWith("'s History")) {
            if (displayName.contains("warning")) openCategory(player, "warnings");
            else if (displayName.contains("mute")) openCategory(player, "mutes");
            else if (displayName.contains("ban")) openCategory(player, "bans");
            else if (displayName.contains("grant")) openCategory(player, "grants");
        }
    }

    public void clearContext(UUID uuid) {
        contexts.remove(uuid);
    }
}