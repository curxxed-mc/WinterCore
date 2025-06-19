package net.curxxed.dev.icore.gui;

import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.NMSUtils;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class RankGUIListener implements Listener {

    private final iCore plugin;

    public RankGUIListener(iCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        // Use reflection to get the inventory title
        String title = NMSUtils.getInventoryTitle(event);

        // Check if the clicked inventory is the "Set rank for" gui
        if (title == null || !title.startsWith("Set rank for ")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String clickedRank = CC.stripColor(clickedItem.getItemMeta().getDisplayName());

        // Handle "Cancel" button click
        if (clickedRank.equalsIgnoreCase("Cancel")) {
            player.closeInventory();
            player.sendMessage(CC.translate("&cRank selection cancelled."));
            return;
        }

        // Get the target player from the stored map
        Player targetPlayer = plugin.getRankManager().getTargetPlayer(player);

        // Ensure the clicked rank exists in the ranks.yml file (case-sensitive)
        if (plugin.getRankManager().getRanksSection().contains(clickedRank)) {
            // Set the target player's rank and notify both players
            plugin.getRankManager().setRank(targetPlayer, clickedRank, player);
            player.closeInventory();
        } else {
            player.sendMessage(CC.translate("&cInvalid rank selected."));
        }
    }
}
