package hyp.ilfov.i.icore.listeners;

import hyp.ilfov.i.icore.Main;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RankGUIListener implements Listener {

    private final Main plugin;

    public RankGUIListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        // Check if the clicked inventory is the "Set Rank for" GUI
        if (!inv.getTitle().startsWith("Set Rank for ")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String clickedRank = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        // Handle "Cancel" button click
        if (clickedRank.equalsIgnoreCase("Cancel")) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Rank selection cancelled.");
            return;
        }

        // Get the target player from the stored map
        Player targetPlayer = plugin.getRankManager().getTargetPlayer(player);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "No target player found.");
            return;
        }

        // Ensure the clicked rank exists in the config (case-sensitive)
        if (plugin.getConfig().getConfigurationSection("ranks").contains(clickedRank)) {
            // Set the target player's rank and notify both players
            plugin.getRankManager().setRank(targetPlayer, clickedRank, player);  // Pass the player who gave the rank
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Invalid rank selected.");
        }
    }
}