package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.commands.staff.ClientBrandCommand;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
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

        plugin.getTasks().timer( this::refreshOnlinePresence, 40L, 100L);
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
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        identityService.recordPlayerIP(uuid, event.getAddress().getHostAddress());
        plugin.getTasks().async(() ->
                networkRedisService.cacheUsername(uuid, playerName)
        );
        try {
            WinterCorePermissibleInjector.initPlayer(player);
        } catch (Exception e) {
            e.printStackTrace();
        }
        denyMaintenanceJoin(event, player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String serverName = plugin.getConfig().getString("server-name", "unknown");

        ClientBrandCommand.silenced.add(uuid);
        joinTimes.put(uuid, System.currentTimeMillis());
        plugin.getTasks().async(() ->
                networkRedisService.setOnlinePresence(uuid, playerName, serverName)
        );

        refreshDisplayForAll(player);
        applyNametag(player);

        plugin.getTasks().later(() -> {
            broadcastStaffJoin(player);
            warnMissingPlaceholderAPI(player);
        }, 5L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        lastSeenTimes.put(uuid, System.currentTimeMillis());
        lastServers.put(uuid, plugin.getConfig().getString("server-name", "unknown"));
        joinTimes.remove(uuid);
        plugin.getTasks().async(() ->
                networkRedisService.clearOnlinePresence(uuid, playerName)
        );

        broadcastStaffQuit(player);
    }

    private void refreshOnlinePresence() {
        String serverName = plugin.getConfig().getString("server-name", "unknown");
        List<OnlinePlayerSnapshot> snapshots = new ArrayList<>();
        for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
            snapshots.add(new OnlinePlayerSnapshot(online.getUniqueId(), online.getName()));
        }

        if (snapshots.isEmpty()) {
            return;
        }

        plugin.getTasks().async(() -> {
            for (OnlinePlayerSnapshot snapshot : snapshots) {
                networkRedisService.setOnlinePresence(snapshot.uuid, snapshot.name, serverName);
            }
        });
    }

    private void refreshDisplayForAll(Player joined) {
        rankManager.refreshPlayerDisplay(joined);
        rankManager.refreshPlayerDisplayForAll(joined);
        for (Player other : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
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
            plugin.getTasks().async(() -> {
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

                plugin.getTasks().sync(() -> {
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

        long quitTime = System.currentTimeMillis();
        plugin.getTasks().async(() ->
                networkRedisService.setStaffLastSeen(uuid, quitTime)
        );

        rankManager.getRank(uuid, rank -> rankManager.getColorPreference(rank, color -> {
            plugin.getTasks().laterAsync(() -> {
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
            for (String line : plugin.getMessageConfig().getList("placeholderapi.missing-warning", Arrays.asList(
                    "&cWarning: PlaceholderAPI is not installed on this server!",
                    "&ePlease install PlaceholderAPI to ensure full functionality.",
                    "&eFor more information, visit: &ahttps://www.spigotmc.org/resources/placeholderapi.6245/"
            ))) {
                player.sendMessage(line);
            }
        }
    }

    private static final class OnlinePlayerSnapshot {
        private final UUID uuid;
        private final String name;

        private OnlinePlayerSnapshot(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    private boolean isStaff(Player player) {
        return player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager")
                || player.isOp();
    }

    private void denyMaintenanceJoin(PlayerLoginEvent event, Player player) {
        if (player.isOp() || player.hasPermission("wintercore.network.maintenance.bypass")) {
            return;
        }

        NetworkRedisService.MaintenanceState state = networkRedisService.getMaintenanceState();
        if (!state.isEnabled()) {
            return;
        }

        List<String> lines = plugin.getMessageConfig().getList("network-maintenance.kick", Arrays.asList(
                "&cThe network is currently under maintenance.",
                "&7Reason: &f{reason}"
        ), "{reason}", state.getReason(),
                "{actor}", state.getActor(),
                "{server}", state.getServerName());

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, CC.translate(String.join("\n", lines)));
    }
}
