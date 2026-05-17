package net.curxxed.dev.wintercore.staff;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StaffModeListener implements Listener {

    private final StaffModeManager staffModeManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 950;

    public StaffModeListener(WinterCore plugin, StaffModeManager manager) {
        this.staffModeManager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffModeManager.isInStaffMode(player)) {
            return;
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack item = player.getItemInHand();
        if (!staffModeManager.isStaffModeItem(item)) {
            return;
        }

        event.setCancelled(true);

        String itemName = item.getItemMeta().getDisplayName();
        Player target = staffModeManager.getTargetPlayer(player);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < COOLDOWN_TIME) return;
        cooldowns.put(uuid, now);
        staffModeManager.handleItemUse(player, itemName, target, item);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
        staffModeManager.handleQuit(event.getPlayer());
    }
}
