package net.curxxed.dev.wintercore.listeners;

import lombok.Getter;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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
    private final PlayerService playerService;
    private final WinterCore plugin;
    @Getter
    public static FreezeListener instance;


    public FreezeListener(PlayerService playerService, WinterCore plugin) {
        this.playerService = playerService;
        this.plugin = plugin;
        instance = this;
    }

    public void freezePlayer(Player player, Player staff) {
        frozenPlayers.add(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        playerService.broadcastFreeze(player, staff,true);
    }

    public void unfreezePlayer(Player player, Player staff) {
        frozenPlayers.remove(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        playerService.broadcastFreeze(player, staff,false);
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
            event.getPlayer().sendMessage(message("moderation.freeze.cannot-interact", "&cYou cannot interact while frozen."));
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
                    attacker.sendMessage(message("moderation.freeze.cannot-attack", "&cYou cannot attack while frozen."));
                    return;
                }

                // If the victim is frozen, cancel the attack and notify the attacker
                if (isFrozen(victim)) {
                    event.setCancelled(true);
                    attacker.sendMessage(message("moderation.freeze.target-cannot-be-attacked",
                            "&c{target} is frozen and cannot be attacked!",
                            "{target}", victim.getName()));
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

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }
}
