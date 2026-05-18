package net.curxxed.dev.wintercore.auth;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

public class AuthListener implements Listener {

    private static final String[] SETUP_ALLOWED_COMMANDS = {"/2fa"};
    private static final String[] AUTH_ALLOWED_COMMANDS = {"/auth", "/2fa"};

    private final AuthManager authManager;
    private final WinterCore plugin;

    public AuthListener(AuthManager authManager, WinterCore plugin) {
        this.authManager = authManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getTasks().later(
                () -> authManager.handleJoin(event.getPlayer()),
                5L
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        authManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String rawCmd = event.getMessage().toLowerCase().trim();

        if (authManager.isPendingSetup(player)) {
            for (String allowed : SETUP_ALLOWED_COMMANDS) {
                if (rawCmd.startsWith(allowed)) return;
            }
            event.setCancelled(true);
            WinterCore winterCore = WinterCore.getInstance();
            if (winterCore != null && winterCore.getMessageConfig() != null) {
                player.sendMessage(winterCore.getMessageConfig().get("auth.setup-required", "&c&l2FA Setup Required"));
            }
            return;
        }

        if (authManager.isPendingAuth(player)) {
            for (String allowed : AUTH_ALLOWED_COMMANDS) {
                if (rawCmd.startsWith(allowed)) return;
            }
            event.setCancelled(true);
            WinterCore winterCore = WinterCore.getInstance();
            if (winterCore != null && winterCore.getMessageConfig() != null) {
                player.sendMessage(winterCore.getMessageConfig().get("auth.auth-required", "&c&lAuthentication Required"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player)) {
            event.setCancelled(true);
            sendAuthMessage(player, "auth.setup-required", "&c&l2FA Setup Required");
            return;
        }
        if (authManager.isPendingAuth(player)) {
            event.setCancelled(true);
            sendAuthMessage(player, "auth.auth-required", "&c&lAuthentication Required");
        }
    }

    private void sendAuthMessage(Player player, String path, String fallback) {
        plugin.getTasks().sync(() -> {
            WinterCore winterCore = WinterCore.getInstance();
            if (player.isOnline() && winterCore != null && winterCore.getMessageConfig() != null) {
                player.sendMessage(winterCore.getMessageConfig().get(path, fallback));
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (authManager.isPendingSetup(player) || authManager.isPendingAuth(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!authManager.isPendingSetup(player) && !authManager.isPendingAuth(player)) return;

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom().clone());
        }
    }
}
