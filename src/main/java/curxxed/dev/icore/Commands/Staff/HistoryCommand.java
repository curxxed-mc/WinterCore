package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.PunishmentManager;
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
    private final Main plugin;

    public HistoryCommand(Main plugin) {
        this.plugin = plugin;
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
        inv.addItem(createGuiItem("Warnings", Material.BOOK, "Click to view warnings.", "warnings"));
        inv.addItem(createGuiItem("Mutes", Material.PAPER, "Click to view mutes.", "mutes"));
        inv.addItem(createGuiItem("Kicks", Material.BARRIER, "Click to view kicks.", "kicks"));
        inv.addItem(createGuiItem("Bans", Material.REDSTONE_BLOCK, "Click to view bans.", "bans"));

        // Open the inventory for the player
        player.openInventory(inv);
    }

    private ItemStack createGuiItem(String name, Material material, String lore, String category) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        meta.setLore(Arrays.asList(ChatColor.GRAY + lore, ChatColor.GRAY + "Category: " + category));
        item.setItemMeta(meta);
        return item;
    }

    public void openPunishmentListGui(Player player, String playerName, String category) {
        Inventory inv = Bukkit.createInventory(null, 36, category + " History");

        PunishmentManager punishmentManager = plugin.getPunishmentManager();
        Map<String, Object> punishments = punishmentManager.getPunishmentData(playerName);
        Map<?, ?> punishmentCategory = (Map<?, ?>) punishments.get(category);

        if (punishmentCategory != null && punishmentCategory.containsKey(playerName)) {
            List<?> list = (List<?>) punishmentCategory.get(playerName);
            if (list != null && !list.isEmpty()) {
                for (Object entry : list) {
                    Map<?, ?> punishment = (Map<?, ?>) entry;
                    String reason = (String) punishment.get("reason");
                    String date = (String) punishment.get("date");
                    String issuer = (String) punishment.get("issuer");
                    String status = getPunishmentStatus(punishment);

                    // Create an item for each punishment
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ChatColor.YELLOW + reason);
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Date: " + date,
                            ChatColor.GRAY + "Issuer: " + issuer,
                            ChatColor.RED + "Status: " + status
                    ));
                    item.setItemMeta(meta);

                    inv.addItem(item);
                }
            } else {
                ItemStack item = new ItemStack(Material.BARRIER);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "No punishments found.");
                item.setItemMeta(meta);
                inv.addItem(item);
            }
        } else {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "No punishments found.");
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }

    private String getPunishmentStatus(Map<?, ?> punishment) {
        String duration = (String) punishment.get("duration");
        if (duration != null && !duration.equalsIgnoreCase("permanent")) {
            return "Expired"; // For simplicity, you can check expiry based on duration
        }
        return "Active";
    }

    // Event handler for inventory clicks to cancel interactions in the punishment history GUI
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
