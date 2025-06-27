package net.curxxed.dev.wintercore.disguise;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseGUI implements Listener {
    private final DisguiseHandler disguiseHandler;
    private static final Map<UUID, String> pendingTargets = new ConcurrentHashMap<>();

    public DisguiseGUI(DisguiseHandler disguiseHandler) {
        this.disguiseHandler = disguiseHandler;
    }

    public static void setPendingTarget(Player player, String targetName) {
        pendingTargets.put(player.getUniqueId(), targetName);
    }
    public static void clearPendingTarget(Player player) {
        pendingTargets.remove(player.getUniqueId());
    }
    public static String getPendingTarget(Player player) {
        return pendingTargets.get(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (!inv.getTitle().equals("Select disguise rank")) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        Player player = (Player) event.getWhoClicked();
        if (clicked == null || !clicked.hasItemMeta()) {
            clearPendingTarget(player);
            player.closeInventory();
            return;
        }
        String rankKey = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        String targetName = getPendingTarget(player);
        if (targetName == null) {
            player.sendMessage(CC.translate("&cNo pending disguise target found."));
            player.closeInventory();
            return;
        }
        try {
            DisguiseCallback result = disguiseHandler.disguise(player, rankKey, targetName, targetName);
            switch (result) {
                case SUCCESS:
                    player.sendMessage(CC.translate("&aDisguise applied as &e" + targetName + " &7with rank &b" + rankKey + "!"));
                    break;
                case ERROR:
                    player.sendMessage(CC.translate("&cFailed to apply disguise. Try again later."));
                    break;
                case NO_RANK_FOUND:
                    player.sendMessage(CC.translate("&cSelected rank is invalid or not found."));
                    break;
                case GLOBAL_PLAYER_FOUND:
                    player.sendMessage(CC.translate("&cA player with that name is already online."));
                    break;
                case SAME_NAME:
                    player.sendMessage(CC.translate("&cYou cannot disguise as yourself."));
                    break;
                case NOT_ONLINE:
                    player.sendMessage(CC.translate("&cYou must be online to disguise."));
                    break;
                default:
                    player.sendMessage(CC.translate("&cUnknown error occurred."));
            }
        } catch (Exception e) {
            player.sendMessage(CC.translate("&cAn error occurred while applying disguise."));
            e.printStackTrace();
        } finally {
            clearPendingTarget(player);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (!inv.getTitle().equals("Select disguise rank")) return;
        Player player = (Player) event.getPlayer();
        clearPendingTarget(player);
    }
}
