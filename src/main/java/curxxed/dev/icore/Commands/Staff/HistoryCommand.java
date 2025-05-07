package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HistoryCommand implements CommandExecutor, Listener {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public HistoryCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /history <player>");
            return false;
        }

        String playerName = args[0];

        // Create the main history GUI
        openHistoryCategoriesGui(player, playerName);
        return true;
    }

    private void openHistoryCategoriesGui(Player player, String playerName) {
        Inventory inv = Bukkit.createInventory(null, 9, "Punishment History");

        // Adding categories to the GUI
        inv.addItem(createGuiItem("Warnings", Material.BOOK, "Click to view warnings.", playerName, "warnings"));
        inv.addItem(createGuiItem("Mutes", Material.PAPER, "Click to view mutes.", playerName, "mutes"));
        inv.addItem(createGuiItem("Kicks", Material.BARRIER, "Click to view kicks.", playerName, "kicks"));
        inv.addItem(createGuiItem("Bans", Material.REDSTONE_BLOCK, "Click to view bans.", playerName, "bans"));

        // Open the inventory for the player
        player.openInventory(inv);
    }

    private ItemStack createGuiItem(String name, Material material, String lore, String playerName, String category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        meta.setLore(Arrays.asList(ChatColor.GRAY + lore, ChatColor.GRAY + "Player: " + playerName, ChatColor.GRAY + "Category: " + category));
        item.setItemMeta(meta);
        return item;
    }

    private void openPunishmentListGui(Player player, String playerName, String category) {
        Inventory inv = Bukkit.createInventory(null, 36, category + " History");

        // Fetch the UUID of the player
        databaseManager.getUUIDByName(playerName, uuid -> {
            if (uuid == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }

            switch (category.toLowerCase()) {
                case "warnings":
                    databaseManager.getWarnings(playerName, warnings -> populatePunishmentGui(inv, warnings));
                    break;
                case "mutes":
                    databaseManager.getMutes(uuid, mutes -> populatePunishmentGui(inv, mutes));
                    break;
                case "kicks":
                    databaseManager.getKicks(playerName, kicks -> populatePunishmentGui(inv, kicks));
                    break;
                case "bans":
                    databaseManager.getBans(playerName, bans -> populatePunishmentGui(inv, bans));
                    break;
            }

            player.openInventory(inv);
        });
    }

    private void populatePunishmentGui(Inventory inv, List<Map<String, String>> punishments) {
        if (punishments.isEmpty()) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No punishments found.");
            item.setItemMeta(meta);
            inv.addItem(item);
        } else {
            for (Map<String, String> punishment : punishments) {
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + punishment.get("reason"));
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Date: " + punishment.get("date"),
                        ChatColor.GRAY + "Issuer: " + punishment.get("issuer")
                ));
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Check if the clicked inventory is the history GUI (for any category)
        if (inv.getTitle().equals("Punishment History") || inv.getTitle().endsWith("History")) {
            event.setCancelled(true); // Prevent interaction with the items

            // Get the ItemMeta of the clicked item to check its lore
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                String category = null;

                // Check the lore to determine which category was clicked
                if (lore.contains("Category: warnings")) {
                    category = "Warnings";
                } else if (lore.contains("Category: mutes")) {
                    category = "Mutes";
                } else if (lore.contains("Category: kicks")) {
                    category = "Kicks";
                } else if (lore.contains("Category: bans")) {
                    category = "Bans";
                }

                if (category != null) {
                    // Open the history for the selected category
                    openPunishmentListGui((Player) event.getWhoClicked(), event.getView().getTitle().split(" ")[0], category);
                }
            }
        }
    }
}