package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ServerSwitchPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.StaffActivityPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.database.service.IdentityService;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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
    private final DisguiseEventListener disguiseEventListener;
    private final NetworkRedisService networkRedisService;

    private final Map<UUID, Long> lastSeenTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastServers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public ConnectionListener(WinterCore plugin, DisguiseEventListener disguiseEventListener, NetworkRedisService networkRedisService) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.identityService = plugin.getDatabaseManager().getIdentityService();
        this.disguiseEventListener = disguiseEventListener;
        this.networkRedisService = networkRedisService;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refreshOnlinePresence, 40L, 100L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        try {
            String rank = plugin.getDatabaseManager().getProfileRepository().getRank(uuid);
            if (rank == null || rank.trim().isEmpty()) {
                rank = "Default";
            }
            plugin.getDatabaseManager().getRankCache().put(uuid, rank);
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        identityService.recordPlayerIP(player.getUniqueId(), event.getAddress().getHostAddress());
        networkRedisService.cacheUsername(player.getUniqueId(), player.getName());
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
        String serverName = plugin.getConfig().getString("server-name", "unknown");

        ClientBrandCommand.silenced.add(uuid);
        joinTimes.put(uuid, System.currentTimeMillis());
        networkRedisService.setOnlinePresence(uuid, player.getName(), serverName);

        refreshDisplayForAll(player);
        applyNametag(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastStaffJoin(player);
            warnMissingPlaceholderAPI(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        lastSeenTimes.put(uuid, System.currentTimeMillis());
        lastServers.put(uuid, plugin.getConfig().getString("server-name", "unknown"));
        joinTimes.remove(uuid);
        networkRedisService.clearOnlinePresence(uuid, player.getName());

        broadcastStaffQuit(player);
    }

    private void refreshOnlinePresence() {
        String serverName = plugin.getConfig().getString("server-name", "unknown");
        for (Player online : Bukkit.getOnlinePlayers()) {
            networkRedisService.setOnlinePresence(online.getUniqueId(), online.getName(), serverName);
        }
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
            });
        } else {
            rankManager.refreshPlayerDisplay(player);
        }
    }

    private void broadcastStaffJoin(Player player) {
        if (!isStaff(player)) return;
        UUID uuid = player.getUniqueId();
        String serverName = plugin.getConfig().getString("server-name", "unknown");
        String realName = resolveRealName(player);

        rankManager.getRank(uuid, rank -> rankManager.getColorPreference(rank, color -> {
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
                networkRedisService.cacheUsername(uuid, realName);

                final String resolvedLastServer = lastServer;
                final String resolvedRealName = realName;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (isSwitch) {
                        disguiseEventListener.onServerSwitch(player);
                        plugin.getRedisManager().publish(new ServerSwitchPacket(
                                serverName, System.currentTimeMillis(),
                                uuid, resolvedLastServer, serverName
                        ));
                        plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                                serverName, System.currentTimeMillis(), "switch",
                                resolvedRealName, color, resolvedLastServer, serverName
                        ));
                    } else {
                        plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                                serverName, System.currentTimeMillis(), "join",
                                resolvedRealName, color, "", serverName
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
        String realName = resolveRealName(player);

        networkRedisService.setStaffLastSeen(uuid, System.currentTimeMillis());

        rankManager.getRank(uuid, rank -> rankManager.getColorPreference(rank, color -> {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                Map<String, String> staffServers = networkRedisService.getStaffLastServers();
                String updatedServer = staffServers.get(uuid.toString());

                if (updatedServer == null || updatedServer.equalsIgnoreCase(currentServer)) {
                    networkRedisService.removeStaffLastServer(uuid);
                    plugin.getRedisManager().publishAndHandleLocally(new StaffActivityPacket(
                            currentServer, System.currentTimeMillis(), "quit",
                            realName, color, currentServer, ""
                    ));
                }
            }, 40L);
        }));
    }

    private String resolveRealName(Player player) {
        UUID uuid = player.getUniqueId();

        String cached = networkRedisService.getCachedUsername(uuid.toString());
        if (cached != null && !cached.trim().isEmpty()) {
            return cached;
        }

        DisguiseData data = plugin.getDisguiseDataMap().get(uuid);
        if (data != null && data.getInfo() != null && data.getInfo().has("name")) {
            try {
                String original = data.getInfo().get("name").getAsString();
                if (original != null && !original.trim().isEmpty()) {
                    return original;
                }
            } catch (Exception ignored) {
            }
        }

        return player.getName();
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
