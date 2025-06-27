package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.EventPriority;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankMenu implements Listener {

    private final WinterCore plugin;
    // Store pending grant state: staff UUID -> GrantState
    private final Map<UUID, GrantState> pendingGrants = new HashMap<>();

    public RankMenu(WinterCore plugin) {
        this.plugin = plugin;
    }

    // Helper class to store grant state
    private static class GrantState {
        public final UUID targetUUID;
        public final String rank;
        public long durationMillis = 0L; // 0 = permanent
        public boolean permanent = false;
        public GrantState(UUID targetUUID, String rank) {
            this.targetUUID = targetUUID;
            this.rank = rank;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        // Use reflection to get the inventory title
        String title = Utilities.getInventoryTitle(event);

        // Duration GUI
        if (title != null && title.startsWith("Set duration for ")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;
            String itemName = CC.stripColor(clickedItem.getItemMeta().getDisplayName());
            GrantState state = pendingGrants.get(player.getUniqueId());
            if (state == null) {
                player.closeInventory();
                player.sendMessage(CC.translate("&cNo pending grant found."));
                return;
            }
            // Handle duration buttons
            switch (itemName) {
                case "+1 Hour": state.durationMillis += 3600_000L; state.permanent = false; break;
                case "+1 Day": state.durationMillis += 86_400_000L; state.permanent = false; break;
                case "+1 Week": state.durationMillis += 604_800_000L; state.permanent = false; break;
                case "+1 Month": state.durationMillis += 2_592_000_000L; state.permanent = false; break;
                case "-1 Hour": state.durationMillis = Math.max(0, state.durationMillis - 3600_000L); state.permanent = false; break;
                case "-1 Day": state.durationMillis = Math.max(0, state.durationMillis - 86_400_000L); state.permanent = false; break;
                case "-1 Week": state.durationMillis = Math.max(0, state.durationMillis - 604_800_000L); state.permanent = false; break;
                case "-1 Month": state.durationMillis = Math.max(0, state.durationMillis - 2_592_000_000L); state.permanent = false; break;
                case "Permanent": state.permanent = true; state.durationMillis = 0L; break;
                case "Continue":
                    player.closeInventory();
                    player.sendMessage(CC.translate("&ePlease type the reason for granting this rank in chat. Type &ccancel &eto abort."));
                    // Listen for chat in a separate listener (not shown here)
                    // Store state in pendingGrants
                    return;
                case "Cancel":
                    pendingGrants.remove(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(CC.translate("&cRank grant cancelled."));
                    return;
            }
            // Reopen duration GUI with updated state
            openDurationGUI(player, state);
            return;
        }

        // Check if the clicked inventory is the "Set rank for" menus
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

        // Get the target UUID from the stored map (works for offline and online players)
        java.util.UUID targetUUID = plugin.getRankManager().getTargetPlayerUUID(player.getUniqueId());
        if (targetUUID == null) {
            player.sendMessage(CC.translate("&cNo target player found for this rank selection."));
            player.closeInventory();
            return;
        }
        // Validate rank exists
        if (plugin.getRankManager().getRanksSection().contains(clickedRank)) {
            // Instead of setting rank immediately, open duration GUI
            GrantState state = new GrantState(targetUUID, clickedRank);
            pendingGrants.put(player.getUniqueId(), state);
            openDurationGUI(player, state);
        } else {
            player.sendMessage(CC.translate("&cInvalid rank selected."));
        }
    }

    // Opens the duration selection GUI
    private void openDurationGUI(Player player, GrantState state) {
        Inventory gui = org.bukkit.Bukkit.createInventory(null, 27, "Set duration for " + org.bukkit.Bukkit.getOfflinePlayer(state.targetUUID).getName());
        // Left: +1h, +1d, +1w, +1m
        gui.setItem(0, createButton(Material.WATCH, CC.Green + "+1 Hour"));
        gui.setItem(1, createButton(Material.WATCH, CC.Green + "+1 Day"));
        gui.setItem(2, createButton(Material.WATCH, CC.Green + "+1 Week"));
        gui.setItem(3, createButton(Material.WATCH, CC.Green + "+1 Month"));
        // Right: -1h, -1d, -1w, -1m
        gui.setItem(5, createButton(Material.WATCH, CC.Red + "-1 Hour"));
        gui.setItem(6, createButton(Material.WATCH, CC.Red + "-1 Day"));
        gui.setItem(7, createButton(Material.WATCH, CC.Red + "-1 Week"));
        gui.setItem(8, createButton(Material.WATCH, CC.Red + "-1 Month"));
        // Center: Permanent
        gui.setItem(13, createButton(Material.BEDROCK, CC.Gold + "Permanent"));
        // Info: current duration
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(CC.Aqua + "Current Duration: " + (state.permanent ? CC.Gold + "Permanent" : CC.Yellow + formatDuration(state.durationMillis)));
        info.setItemMeta(meta);
        gui.setItem(22, info);
        // Continue button
        ItemStack cont = new ItemStack(Material.WOOL, 1, (short) 5);
        ItemMeta contMeta = cont.getItemMeta();
        contMeta.setDisplayName(CC.Green + "Continue");
        cont.setItemMeta(contMeta);
        gui.setItem(26, cont);
        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(CC.Red + "Cancel");
        cancel.setItemMeta(cancelMeta);
        gui.setItem(18, cancel);
        player.openInventory(gui);
    }

    private ItemStack createButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0";
        long hours = millis / 3600000;
        return hours + "h";
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GrantState state = pendingGrants.get(player.getUniqueId());
        if (state == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            pendingGrants.remove(player.getUniqueId());
            player.sendMessage(CC.translate("&cRank grant cancelled."));
            return;
        }
        // Finalize grant
        pendingGrants.remove(player.getUniqueId());
        long now = System.currentTimeMillis();
        Long expiresAt = state.permanent ? null : now + state.durationMillis;
        // Save to player_ranks (current)
        plugin.getDatabaseManager().setRankWithMeta(state.targetUUID, state.rank, player.getUniqueId(), now, expiresAt, message);
        // Save to player_rank_grants (history)
        plugin.getDatabaseManager().addRankGrant(state.targetUUID, state.rank, player.getUniqueId(), now, expiresAt, message);
        player.sendMessage(CC.translate("&aGranted rank &e" + state.rank + " &ato &b" + plugin.getServer().getOfflinePlayer(state.targetUUID).getName() + "&a."));
        // Fire RankChangeEvent for placeholder update
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(state.targetUUID);
            if (target != null) {
                net.curxxed.dev.wintercore.rank.RankChangeEvent eventRank = new net.curxxed.dev.wintercore.rank.RankChangeEvent(target, state.rank);
                org.bukkit.Bukkit.getPluginManager().callEvent(eventRank);
            }
        });
    }

    // Add this public method
    public boolean isPendingGrant(UUID staffUUID) {
        return pendingGrants.containsKey(staffUUID);
    }
}
