package net.curxxed.dev.wintercore.player;

import net.curxxed.dev.wintercore.config.ModerationMessages;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BanList implements Listener {

    private static final long REMINDER_COOLDOWN_MS = 1500L;

    private final WinterCore plugin;
    private final ModerationService moderationService;
    private final ConcurrentMap<UUID, BanState> banStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastReminderTimes = new ConcurrentHashMap<>();

    public BanList(WinterCore plugin) {
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        cacheBanState(event.getUniqueId(), moderationService.getActiveBan(event.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BanState state = getBanState(player.getUniqueId());

        if (state != null) {
            notifyBannedPlayer(player, state);
            refreshBanStateAsync(player.getUniqueId(), false);
            return;
        }

        refreshBanStateAsync(player.getUniqueId(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastReminderTimes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (restrict(event.getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (restrict(event.getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (restrict(event.getPlayer(), true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (restrict(event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && restrict((Player) event.getPlayer(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && restrict((Player) event.getWhoClicked(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player && restrict((Player) event.getWhoClicked(), false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player player = extractDamager(event.getDamager());
        if (player != null && restrict(player, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() == null) {
            return;
        }

        Object shooter = ((Projectile) event.getEntity()).getShooter();
        if (shooter instanceof Player && restrict((Player) shooter, false)) {
            event.setCancelled(true);
        }
    }

    public void applyBan(UUID uuid, String reason, Long expiresAt, boolean notifyIfOnline) {
        banStates.put(uuid, new BanState(reason, expiresAt));
        if (!notifyIfOnline) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            notifyBannedPlayer(player, getBanState(uuid));
        }
    }

    public void removeBan(UUID uuid) {
        banStates.remove(uuid);
        lastReminderTimes.remove(uuid);
    }

    private void refreshBanStateAsync(UUID uuid, boolean notifyIfOnline) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ModerationService.ActiveBan activeBan = moderationService.getActiveBan(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                cacheBanState(uuid, activeBan);
                if (!notifyIfOnline || activeBan == null) {
                    return;
                }

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    notifyBannedPlayer(player, getBanState(uuid));
                }
            });
        });
    }

    private void cacheBanState(UUID uuid, ModerationService.ActiveBan activeBan) {
        if (activeBan == null) {
            removeBan(uuid);
            return;
        }

        banStates.put(uuid, new BanState(activeBan.getReason(), activeBan.getExpiration()));
    }

    private boolean restrict(Player player, boolean closeInventory) {
        BanState state = getBanState(player.getUniqueId());
        if (state == null) {
            return false;
        }

        if (closeInventory) {
            runSync(player::closeInventory);
        }

        sendReminder(player, state);
        return true;
    }

    private void notifyBannedPlayer(Player player, BanState state) {
        if (state == null) {
            return;
        }

        runSync(() -> {
            player.closeInventory();
            player.sendMessage(ModerationMessages.formatJoinRestrictionMessage(state.reason, state.expiresAt));
        });
    }

    private void sendReminder(Player player, BanState state) {
        long now = System.currentTimeMillis();
        Long lastSent = lastReminderTimes.get(player.getUniqueId());
        if (lastSent != null && now - lastSent < REMINDER_COOLDOWN_MS) {
            return;
        }

        lastReminderTimes.put(player.getUniqueId(), now);
        runSync(() -> player.sendMessage(ModerationMessages.formatRestrictionReminder(state.reason, state.expiresAt)));
    }

    private BanState getBanState(UUID uuid) {
        BanState state = banStates.get(uuid);
        if (state == null) {
            return null;
        }

        if (state.isExpired()) {
            removeBan(uuid);
            return null;
        }

        return state;
    }

    private Player extractDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) {
            return (Player) entity;
        }

        if (entity instanceof Projectile) {
            Object shooter = ((Projectile) entity).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }

        return null;
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private static final class BanState {
        private final String reason;
        private final Long expiresAt;

        private BanState(String reason, Long expiresAt) {
            this.reason = reason == null || reason.trim().isEmpty() ? "No reason provided" : reason;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return expiresAt != null && expiresAt <= System.currentTimeMillis();
        }
    }
}
