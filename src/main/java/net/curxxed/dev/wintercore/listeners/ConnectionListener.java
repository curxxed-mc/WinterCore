package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ServerSwitchPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.StaffActivityPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.database.service.IdentityService;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final IdentityService identityService;
    private final ModerationService moderationService;
    private final DisguiseEventListener disguiseEventListener;
    private final NetworkRedisService networkRedisService;

    private final Map<UUID, Long> lastSeenTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastServers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public ConnectionListener(WinterCore plugin, DisguiseEventListener disguiseEventListener, NetworkRedisService networkRedisService) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.identityService = plugin.getDatabaseManager().getIdentityService();
        this.moderationService = plugin.getDatabaseManager().getModerationService();
        this.disguiseEventListener = disguiseEventListener;
        this.networkRedisService = networkRedisService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        identityService.recordPlayerIP(player.getUniqueId(), event.getAddress().getHostAddress());
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
        joinTimes.put(uuid, System.currentTimeMillis());

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

        lastSeenTimes.put(uuid, System.currentTimeMillis());
        lastServers.put(uuid, plugin.getConfig().getString("server-name", "unknown"));
        joinTimes.remove(uuid);

        broadcastStaffQuit(player);
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
        moderationService.getBanDetails(player.getUniqueId(), banDetails -> {
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
        String serverName = plugin.getConfig().getString("server-name", "unknown");

        rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, color -> {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Map<String, String> staffServers = networkRedisService.getStaffLastServers();
                String lastServer = staffServers.get(uuid.toString());
                long lastSeen = networkRedisService.getStaffLastSeen(uuid);
                long joinTime = joinTimes.getOrDefault(uuid, System.currentTimeMillis());

                boolean isSwitch = lastServer != null
                        && !lastServer.equalsIgnoreCase(serverName)
                        && lastSeen > 0
                        && (joinTime - lastSeen < 5000);

                networkRedisService.setStaffLastServer(uuid, serverName);
                networkRedisService.cacheUsername(uuid, player.getName());

                String realName = player.getName();
                final String resolvedLastServer = lastServer;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (isSwitch) {
                        disguiseEventListener.onServerSwitch(player);
                        plugin.getRedisManager().publish(new ServerSwitchPacket(
                                serverName, System.currentTimeMillis(),
                                uuid, resolvedLastServer, serverName
                        ));
                        plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                                serverName, System.currentTimeMillis(), "switch",
                                realName, color, resolvedLastServer, serverName
                        ));
                    } else {
                        plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                                serverName, System.currentTimeMillis(), "join",
                                realName, color, "", serverName
                        ));
                    }
                });
            });
        }));
    }

    private void broadcastStaffQuit(Player player) {
        if (!isStaff(player)) return;
        UUID uuid = player.getUniqueId();
        String currentServer = plugin.getConfig().getString("server-name", "unknown");

        networkRedisService.setStaffLastSeen(uuid, System.currentTimeMillis());

        rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, color -> {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                Map<String, String> staffServers = networkRedisService.getStaffLastServers();
                String updatedServer = staffServers.get(uuid.toString());

                if (updatedServer == null || updatedServer.equalsIgnoreCase(currentServer)) {
                    networkRedisService.removeStaffLastServer(uuid);
                    plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                            currentServer, System.currentTimeMillis(), "quit",
                            player.getName(), color, currentServer, ""
                    ));
                }
            }, 40L);
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