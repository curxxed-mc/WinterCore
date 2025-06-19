package net.curxxed.dev.icore.listeners;

import lombok.Getter;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class FreezeListener implements Listener {
    private final Set<Player> frozenPlayers = new HashSet<>();
    private final PlayerListener playerListener;
    @Getter
    public static FreezeListener instance;


    public FreezeListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
        instance = this;
    }

    public void freezePlayer(Player player, Player staff) {
        frozenPlayers.add(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        playerListener.sendFreezeNotification(player, staff,true);
    }

    public void unfreezePlayer(Player player, Player staff) {
        frozenPlayers.remove(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        playerListener.sendFreezeNotification(player, staff,false);
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player);
    }

    public boolean isPlayerFrozen(Player player) {
        return instance != null && instance.isFrozen(player);
    }

    // Prevents Movement
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom()); // Cancel movement while preserving yaw/pitch
            }
        }
    }

    // Prevents Interactions
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.translate("&cYou cannot interact while frozen."));
        }
    }

    // Prevents Dropping Items
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Prevents Picking Up Items
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupItem(PlayerPickupItemEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Prevents Inventory Click
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Prevents Block Breaking
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Prevents Block Placing
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // Prevents Attacking and Getting Hit
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (event.getEntity() instanceof Player) {
                Player victim = (Player) event.getEntity();

                // If the attacker is frozen, cancel their attack
                if (isFrozen(attacker)) {
                    event.setCancelled(true);
                    attacker.sendMessage(CC.translate("&cYou cannot attack while frozen."));
                    return;
                }

                // If the victim is frozen, cancel the attack and notify the attacker
                if (isFrozen(victim)) {
                    event.setCancelled(true);
                    attacker.sendMessage(CC.translate("&c" + victim.getName() + " is frozen and cannot be attacked!"));
                }
            }
        }
    }

    // Prevents Shooting Arrows or Other Projectiles
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Prevents Hunger Depletion
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }
}
