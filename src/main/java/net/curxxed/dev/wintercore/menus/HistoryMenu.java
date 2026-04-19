package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.menu.Button;
import net.curxxed.dev.wintercore.menu.Menu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HistoryMenu extends Menu {

    private enum View { MAIN, CATEGORY }

    private final WinterCore plugin;
    private final DatabaseManager db;
    private final MenuConfig menuConfig;
    private final String targetName;
    private final UUID targetUuid;

    private View view = View.MAIN;
    private String categoryTitle = "";
    private List<ItemStack> categoryItems = new ArrayList<>();
    private int page = 0;

    public HistoryMenu(WinterCore plugin, MenuConfig menuConfig, String targetName, UUID targetUuid) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.menuConfig = menuConfig;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
    }

    @Override
    public String getTitle() {
        return view == View.MAIN
                ? menuConfig.getTitle("history-menu", targetName)
                : categoryTitle;
    }

    @Override
    public int getSize() {
        return view == View.MAIN
                ? menuConfig.getSize("history-menu")
                : menuConfig.getSize("history-category");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return view == View.MAIN ? buildMainButtons(player) : buildCategoryButtons(player);
    }

    private Map<Integer, Button> buildMainButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigurationSection items = menuConfig.getSection("history-menu.items");
        if (items == null) return buttons;

        for (String key : items.getKeys(false)) {
            int slot = menuConfig.getSlot("history-menu.items." + key);
            String lower = key.toLowerCase();

            String category;
            if      (lower.contains("warning")) category = "warnings";
            else if (lower.contains("mute"))    category = "mutes";
            else if (lower.contains("ban"))     category = "bans";
            else if (lower.contains("grant"))   category = "grants";
            else                                category = null;

            final String cat = category;
            buttons.put(slot, new Button(
                    menuConfig.buildItem("history-menu.items." + key),
                    cat != null ? e -> openCategory(player, cat) : null
            ));
        }
        return buttons;
    }

    private Map<Integer, Button> buildCategoryButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int perPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) categoryItems.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * perPage;
        int end = Math.min(start + perPage, categoryItems.size());
        for (int i = start; i < end; i++) {
            buttons.put(i - start, new Button(categoryItems.get(i)));
        }

        if (page > 0) {
            buttons.put(46, new Button(navItem("&aPrevious Page"), e -> { page--; refresh(player); }));
        }
        if (page < totalPages - 1) {
            buttons.put(52, new Button(navItem("&aNext Page"), e -> { page++; refresh(player); }));
        }
        buttons.put(49, new Button(navItem("&ePage " + (page + 1) + " of " + totalPages)));

        buttons.put(menuConfig.getSlot("history-category.back-button"), new Button(
                menuConfig.buildItem("history-category.back-button"),
                e -> {
                    view = View.MAIN;
                    page = 0;
                    categoryItems.clear();
                    open(player);
                }
        ));

        return buttons;
    }

    private void openCategory(Player player, String category) {
        switch (category) {
            case "warnings":
                db.getModerationService().getWarnings(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.warning-item",
                                "&7Reason: "  + map.getOrDefault("reason", "Unknown"),
                                "&7Issuer: "  + map.getOrDefault("issuer", "Unknown"),
                                "&7Date: "    + map.getOrDefault("date",   "Unknown")));
                    }
                    showCategory(player, items,
                            menuConfig.getTitle("history-menu", targetName)
                                    .replace(targetName + "'s History", targetName + "'s Warnings"));
                });
                break;
            case "mutes":
                db.getModerationService().getMutes(targetUuid, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.mute-item",
                                "&7Reason: "  + map.getOrDefault("reason",     "Unknown"),
                                "&7Issuer: "  + map.getOrDefault("issuer",     "Unknown"),
                                "&7Expires: " + map.getOrDefault("expiration", "Unknown")));
                    }
                    showCategory(player, items, CC.translate("&6" + targetName + "'s Mutes"));
                });
                break;
            case "bans":
                db.getModerationService().getBans(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.ban-item",
                                "&7Reason: "  + map.getOrDefault("reason",     "Unknown"),
                                "&7Issuer: "  + map.getOrDefault("issuer",     "Unknown"),
                                "&7Date: "    + map.getOrDefault("date",       "Unknown"),
                                "&7Expires: " + map.getOrDefault("expiration", "Permanent")));
                    }
                    showCategory(player, items, CC.translate("&6" + targetName + "'s Bans"));
                });
                break;
            case "grants":
                db.getModerationService().getRankGrants(targetUuid, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        items.add(buildEntryItem("history-category.grant-item",
                                "&7Rank: "       + map.getOrDefault("rank",       "Unknown"),
                                "&7Granted by: " + map.getOrDefault("issuer",     "Unknown"),
                                "&7Date: "       + map.getOrDefault("date",       "Unknown"),
                                "&7Expires: "    + map.getOrDefault("expiration", "Permanent"),
                                "&7Reason: "     + map.getOrDefault("reason",     "Unknown")));
                    }
                    showCategory(player, items, CC.translate("&6" + targetName + "'s Grants"));
                });
                break;
        }
    }

    private void showCategory(Player player, List<ItemStack> items, String title) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            this.view = View.CATEGORY;
            this.categoryTitle = title;
            this.categoryItems = items;
            this.page = 0;
            open(player);
        });
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

    private ItemStack navItem(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate(name));
        item.setItemMeta(meta);
        return item;
    }
}