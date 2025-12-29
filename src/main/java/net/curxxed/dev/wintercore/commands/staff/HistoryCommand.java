package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.menus.HistroyMenuContext;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@CommandInfo(
        name = "history",
        permission = "WinterCore.history",
        description = "View a player's punishment and grant history.",
        usage = "/history <player>",
        async = true
)
public class HistoryCommand extends BaseCommand implements Listener {

    private final Map<UUID, HistroyMenuContext> menuContexts = new HashMap<>();

    public HistoryCommand(WinterCore plugin) {
        super(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(CommandArguments args) {
        if (args.length() < 1) {
            args.getSender().sendMessage(CC.translate("&cUsage: " + commandInfo.usage()));
            return;
        }

        if (!args.isPlayer()) {
            args.getSender().sendMessage(CC.translate("&cOnly players can open the history menu."));
            return;
        }

        Player sender = args.getPlayer();
        String targetName = args.getArgs()[0];

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(CC.translate("&cPlayer not found."));
            return;
        }

        openHistoryMain(sender, target.getName(), target.getUniqueId());
    }

    private void openHistoryMain(Player viewer, String targetName, UUID targetUuid) {
        Inventory inv = Bukkit.createInventory(null, 27, CC.translate("&6" + targetName + "'s History"));

        // Layout the main category items
        inv.setItem(10, createHistoryItem(Material.PAPER, "&eWarnings", "&7Click to view warnings."));
        inv.setItem(12, createHistoryItem(Material.BOOK, "&6Mutes", "&7Click to view mutes."));
        inv.setItem(14, createHistoryItem(Material.IRON_DOOR, "&cBans", "&7Click to view bans."));
        inv.setItem(16, createHistoryItem(Material.NAME_TAG, "&aGrants", "&7Click to view rank grants."));

        menuContexts.put(viewer.getUniqueId(), new HistroyMenuContext(targetName, targetUuid));

        // Return to main thread for inventory opening
        Bukkit.getScheduler().runTask(plugin, () -> viewer.openInventory(inv));
    }

    private ItemStack createHistoryItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.translate(name));
            meta.setLore(Collections.singletonList(CC.translate(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openCategoryHistory(Player viewer, String category, String targetName, UUID targetUuid) {
        DatabaseManager db = plugin.getDatabaseManager();

        // Delegate fetching to DatabaseManager to avoid SQL in command classes
        switch (category.toLowerCase()) {
            case "warnings":
                db.getWarnings(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        ItemStack item = new ItemStack(Material.BOOK);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(CC.translate("&eWarning"));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Reason: " + map.getOrDefault("reason", "Unknown")));
                            lore.add(CC.translate("&7Issuer: " + map.getOrDefault("issuer", "Unknown")));
                            lore.add(CC.translate("&7Date: " + map.getOrDefault("date", "Unknown")));
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.translate("&6" + targetName + "'s Warnings"));
                });
                break;
            case "mutes":
                db.getMutes(targetUuid, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        ItemStack item = new ItemStack(Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(CC.translate("&bMute"));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Reason: " + map.getOrDefault("reason", "Unknown")));
                            lore.add(CC.translate("&7Issuer: " + map.getOrDefault("issuer", "Unknown")));
                            lore.add(CC.translate("&7Expires: " + map.getOrDefault("expiration", "Unknown")));
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.translate("&6" + targetName + "'s Mutes"));
                });
                break;
            case "bans":
                db.getBans(targetName, maps -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Map<String, String> map : maps) {
                        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName(CC.translate("&cBan"));
                            List<String> lore = new ArrayList<>();
                            lore.add(CC.translate("&7Reason: " + map.getOrDefault("reason", "Unknown")));
                            lore.add(CC.translate("&7Date: " + map.getOrDefault("date", "Unknown")));
                            lore.add(CC.translate("&7Expires: " + map.getOrDefault("expiration", "Unknown")));
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        items.add(item);
                    }
                    openPaginatedHistory(viewer, items, CC.translate("&6" + targetName + "'s Bans"));
                });
                break;
            case "grants":
               // db.getGrants(targetUuid, items -> openPaginatedHistory(viewer, items, CC.translate("&6" + targetName + "'s Grants")));
                break;
        }
    }

    private void openPaginatedHistory(Player viewer, List<ItemStack> items, String title) {
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill up to 45 items (leaving space for navigation/back buttons)
        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            inv.setItem(i, items.get(i));
        }

        // Standardized back button at bottom middle
        inv.setItem(49, createHistoryItem(Material.ARROW, "&cBack", "&7Return to main history menu."));

        Bukkit.getScheduler().runTask(plugin, () -> viewer.openInventory(inv));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = Utilities.getInventoryTitle(event);
        if (title == null) return;

        HistroyMenuContext context = menuContexts.get(player.getUniqueId());
        if (context == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Categorize and handle clicks
        boolean isHistoryMenu = title.contains("'s History") || title.contains("'s Warnings") ||
                title.contains("'s Mutes") || title.contains("'s Bans") ||
                title.contains("'s Grants");

        if (isHistoryMenu) {
            event.setCancelled(true);
        } else {
            return;
        }

        // Handle Back Button functionality
        if (clickedItem.getType() == Material.ARROW) {
            openHistoryMain(player, context.getPlayerName(), context.getUuid());
            return;
        }

        // Process Category selection in the main view
        if (title.endsWith("'s History")) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            String stripped = org.bukkit.ChatColor.stripColor(meta.getDisplayName()).toLowerCase();

            if (stripped.contains("warnings")) {
                context.setCurrentCategory("Warnings");
                openCategoryHistory(player, "Warnings", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("mutes")) {
                context.setCurrentCategory("Mutes");
                openCategoryHistory(player, "Mutes", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("bans")) {
                context.setCurrentCategory("Bans");
                openCategoryHistory(player, "Bans", context.getPlayerName(), context.getUuid());
            } else if (stripped.contains("grants")) {
                context.setCurrentCategory("Grants");
                openCategoryHistory(player, "Grants", context.getPlayerName(), context.getUuid());
            }
        }
    }
}
