package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.events.ServerSwitchEvent;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final DatabaseManager databaseManager;

    public ConnectionListener(WinterCore plugin) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        databaseManager.recordPlayerIP(player.getUniqueId(), event.getAddress().getHostAddress());
        try {
            WinterCorePermissibleInjector.initPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ClientBrandCommand.silenced.add(uuid);

        refreshDisplayForAll(player);
        applyNametag(player);
        checkBan(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyNametag(player);
            broadcastStaffJoin(player);
            warnMissingPlaceholderAPI(player);
        }, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);
            plugin.getRedisManager().updateLastSeen(uuid);

            if (isPending) {
                plugin.getRedisManager().clearPendingSwitch(uuid);
                return;
            }

            broadcastStaffQuit(player);
        }, 2L);
    }

    private void refreshDisplayForAll(Player joined) {
        rankManager.refreshPlayerDisplay(joined);
        rankManager.refreshPlayerDisplayForAll(joined);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(joined.getUniqueId())) {
                rankManager.refreshPlayerDisplayForAll(other);
            }
        }
    }

    private void applyNametag(Player player) {
        if (plugin.getNameTagColorManager() == null) return;

        if (plugin.getDisguiseRegistry().isDisguised(player)) {
            plugin.getDisguiseRegistry().getEffectiveColor(player, color -> {
                plugin.getNameTagColorManager().applyColor(player, color);
                String formatted = CC.translate(color) + player.getName() + ChatColor.RESET;
                player.setPlayerListName(formatted);
            });
        } else {
            rankManager.refreshPlayerDisplay(player);
        }
    }

    private void checkBan(Player player) {
        databaseManager.getBanDetails(player.getUniqueId(), banDetails -> {
            if (banDetails == null || banDetails.isEmpty()) return;
            Long expiration = (Long) banDetails.get("expiration");
            String reason = (String) banDetails.get("reason");
            String reasonText = reason != null ? reason : "No reason provided";

            if (expiration != null) {
                long timeLeft = expiration - System.currentTimeMillis();
                String timeMessage;
                if (timeLeft < 60000) timeMessage = (timeLeft / 1000) + " seconds";
                else if (timeLeft < 3600000) timeMessage = (timeLeft / 60000) + " minutes";
                else if (timeLeft < 86400000) timeMessage = (timeLeft / 3600000) + " hours";
                else timeMessage = (timeLeft / 86400000) + " days";
                player.sendMessage(CC.translate("&cYou are temporarily banned for: " + timeMessage + "\n&cReason: &b" + reasonText));
            } else {
                player.sendMessage(CC.translate("&cYou are permanently banned.\n&cReason: &b" + reasonText));
            }
        });
    }

    private void broadcastStaffJoin(Player player) {
        if (!isStaff(player)) return;
        UUID uuid = player.getUniqueId();
        rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, color -> {
            String serverName = plugin.getConfig().getString("server-name", "hub-restricted");
            String last = plugin.getRedisManager().getLastServer(uuid);
            long lastSeen = plugin.getRedisManager().getLastSeen(uuid);
            boolean isSwitch = last != null && !last.equals(serverName) && (System.currentTimeMillis() - lastSeen < 30000);

            plugin.getRedisManager().updateLastServer(uuid, serverName);

            String realName = player.getName();
            if (isSwitch) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.getPluginManager().callEvent(new ServerSwitchEvent(player, last, serverName))
                );
                plugin.getRedisManager().publishStaffActivity("switch", realName, color, last, serverName);
            } else {
                plugin.getRedisManager().publishStaffActivity("join", realName, color, "", serverName);
            }
        }));
    }

    private void broadcastStaffQuit(Player player) {
        if (!isStaff(player)) return;
        UUID uuid = player.getUniqueId();
        rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, color -> {
            String lastServer = plugin.getRedisManager().getLastServer(uuid);
            if (lastServer == null) lastServer = "unknown";
            plugin.getRedisManager().publishStaffActivity(
                    "quit", player.getName(), color,
                    plugin.getConfig().getString("server-name"), lastServer
            );
        }));
    }

    private void warnMissingPlaceholderAPI(Player player) {
        if (!plugin.isPlaceholderAPIEnabled()
                && (player.hasPermission("wintercore.admin") || player.hasPermission("wintercore.manager"))) {
            player.sendMessage(CC.translate("&cWarning: PlaceholderAPI is not installed on this server!"));
            player.sendMessage(CC.translate("&ePlease install PlaceholderAPI to ensure full functionality."));
            player.sendMessage(CC.translate("&eFor more information, visit: &ahttps://www.spigotmc.org/resources/placeholderapi.6245/"));
        }
    }

    private boolean isStaff(Player player) {
        return player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager")
                || player.isOp();
    }
}