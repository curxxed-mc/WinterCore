package curxxed.dev.icore.utils.GUI;

import curxxed.dev.icore.utils.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

public class DisguiseGUI implements Listener {

    private final RankManager rankManager;

    public DisguiseGUI(RankManager rankManager) {
        this.rankManager = rankManager;
    }

    // Method to open the disguise GUI
    public void openDisguiseGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, ChatColor.GREEN + "Disguise GUI");

        // Example: Add a rank selection item
        ItemStack rankItem = new ItemStack(Material.DIAMOND_SWORD); // Example material
        ItemMeta rankMeta = rankItem.getItemMeta();
        rankMeta.setDisplayName(ChatColor.YELLOW + "Select Rank");
        rankItem.setItemMeta(rankMeta);

        inventory.setItem(4, rankItem); // Put it in the center

        // Add other items for skin, name, etc. as needed

        player.openInventory(inventory);
    }

    // Handle inventory click events
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the clicked inventory is the disguise GUI
        if (event.getInventory().getName().equals(ChatColor.GREEN + "Disguise GUI")) {
            event.setCancelled(true); // Prevent item movement

            // Get the clicked item
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String itemName = clickedItem.getItemMeta().getDisplayName();

            // Handle clicks for different items (e.g., selecting rank, name, skin)
            if (itemName.equals(ChatColor.YELLOW + "Select Rank")) {
                // Trigger the rank selection logic (maybe open a new GUI or update the player's disguise)
                // For example, you can call `rankManager.getSortedRanks()`
            }
            // Add other checks for skin, name, etc.
        }
    }

    // Handle inventory close events
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Check if the inventory that was closed is the disguise GUI
        if (event.getInventory().getName().equals(ChatColor.GREEN + "Disguise GUI")) {
            Player player = (Player) event.getPlayer();
            // Optionally, handle saving the player's disguise or resetting state
        }
    }
}
