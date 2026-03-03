package net.curxxed.dev.wintercore.menus;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankMenu implements Listener {

    private final WinterCore plugin;
    private final Map<UUID, GrantState> pendingGrants = new HashMap<>();

    public RankMenu(WinterCore plugin) {
        this.plugin = plugin;
    }

    private static class GrantState {
        public final UUID targetUUID;
        public final String rank;
        public long durationMillis = 0L;
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
        String title = Utilities.getInventoryTitle(event);

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

            switch (itemName) {
                case "+1 Hour":   state.durationMillis += 3_600_000L;    state.permanent = false; break;
                case "+1 Day":    state.durationMillis += 86_400_000L;   state.permanent = false; break;
                case "+1 Week":   state.durationMillis += 604_800_000L;  state.permanent = false; break;
                case "+1 Month":  state.durationMillis += 2_592_000_000L; state.permanent = false; break;
                case "-1 Hour":   state.durationMillis = Math.max(0, state.durationMillis - 3_600_000L);    state.permanent = false; break;
                case "-1 Day":    state.durationMillis = Math.max(0, state.durationMillis - 86_400_000L);   state.permanent = false; break;
                case "-1 Week":   state.durationMillis = Math.max(0, state.durationMillis - 604_800_000L);  state.permanent = false; break;
                case "-1 Month":  state.durationMillis = Math.max(0, state.durationMillis - 2_592_000_000L); state.permanent = false; break;
                case "Permanent": state.permanent = true; state.durationMillis = 0L; break;
                case "Continue":
                    player.closeInventory();
                    player.sendMessage(CC.translate("&ePlease type the reason for granting this rank in chat. Type &ccancel &eto abort."));
                    return;
                case "Cancel":
                    pendingGrants.remove(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(CC.translate("&cRank grant cancelled."));
                    return;
            }
            openDurationGUI(player, state);
            return;
        }

        if (title == null || !title.startsWith("Set rank for ")) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String clickedRank = CC.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (clickedRank.equalsIgnoreCase("Cancel")) {
            player.closeInventory();
            player.sendMessage(CC.translate("&cRank selection cancelled."));
            return;
        }

        UUID targetUUID = plugin.getRankManager().getTargetPlayerUUID(player.getUniqueId());
        if (targetUUID == null) {
            player.sendMessage(CC.translate("&cNo target player found for this rank selection."));
            player.closeInventory();
            return;
        }

        if (plugin.getRankManager().getRanksSection().contains(clickedRank)) {
            GrantState state = new GrantState(targetUUID, clickedRank);
            pendingGrants.put(player.getUniqueId(), state);
            openDurationGUI(player, state);
        } else {
            player.sendMessage(CC.translate("&cInvalid rank selected."));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
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

        pendingGrants.remove(player.getUniqueId());

        long now = System.currentTimeMillis();
        Long expiresAt = state.permanent ? null : (state.durationMillis > 0 ? now + state.durationMillis : null);

        plugin.getDatabaseManager().setRankWithMeta(state.targetUUID, state.rank, player.getUniqueId(), now, expiresAt, message);
        plugin.getDatabaseManager().addRankGrant(state.targetUUID, state.rank, player.getUniqueId(), now, expiresAt, message);

        String targetName = Bukkit.getOfflinePlayer(state.targetUUID).getName();
        player.sendMessage(CC.translate("&aGranted rank &e" + state.rank + " &ato &b" + targetName + "&a."));

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayer(state.targetUUID);
            if (target != null) {
                plugin.getRankManager().cachePlayerRank(target, state.rank);
                plugin.getRankManager().refreshPlayerDisplay(target);
                plugin.getRankManager().refreshPlayerDisplayForAll(target);
                Bukkit.getPluginManager().callEvent(
                        new net.curxxed.dev.wintercore.rank.RankChangeEvent(target, state.rank)
                );
            }
        });
    }

    public boolean isPendingGrant(UUID staffUUID) {
        return pendingGrants.containsKey(staffUUID);
    }

    private void openDurationGUI(Player player, GrantState state) {
        Inventory gui = Bukkit.createInventory(null, 27, "Set duration for " + Bukkit.getOfflinePlayer(state.targetUUID).getName());

        gui.setItem(0, createButton(Material.WATCH, CC.translate("&a+1 Hour")));
        gui.setItem(1, createButton(Material.WATCH, CC.translate("&a+1 Day")));
        gui.setItem(2, createButton(Material.WATCH, CC.translate("&a+1 Week")));
        gui.setItem(3, createButton(Material.WATCH, CC.translate("&a+1 Month")));
        gui.setItem(5, createButton(Material.WATCH, CC.translate("&c-1 Hour")));
        gui.setItem(6, createButton(Material.WATCH, CC.translate("&c-1 Day")));
        gui.setItem(7, createButton(Material.WATCH, CC.translate("&c-1 Week")));
        gui.setItem(8, createButton(Material.WATCH, CC.translate("&c-1 Month")));
        gui.setItem(13, createButton(Material.BEDROCK, CC.translate("&6Permanent")));

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(CC.translate("&bCurrent Duration: " + (state.permanent ? "&6Permanent" : "&e" + formatDuration(state.durationMillis))));
        info.setItemMeta(infoMeta);
        gui.setItem(22, info);

        ItemStack cont = new ItemStack(Material.WOOL, 1, (short) 5);
        ItemMeta contMeta = cont.getItemMeta();
        contMeta.setDisplayName(CC.translate("&aContinue"));
        cont.setItemMeta(contMeta);
        gui.setItem(26, cont);

        ItemStack cancel = Utilities.IS_1_7
                ? new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14)
                : new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(CC.translate("&cCancel"));
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
        long hours = millis / 3_600_000L;
        return hours + "h";
    }
}