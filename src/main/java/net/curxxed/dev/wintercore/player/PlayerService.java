package net.curxxed.dev.wintercore.player;

import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerReportPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerUpdatePacket;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.database.service.ProfileService;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerService implements Listener {

    private final WinterCore plugin;
    private final ModerationService moderationService;
    private final ProfileService profileService;
    private final Map<UUID, WinterCorePlayer> cache = new ConcurrentHashMap<>();

    public PlayerService(WinterCore plugin) {
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
        this.profileService = plugin.getDatabaseManager().getProfileService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    public void loadPlayerData(UUID uuid, String name) {
        WinterCorePlayer wcPlayer = cache.computeIfAbsent(uuid, k -> new WinterCorePlayer(uuid, name));
        profileService.getRank(uuid, rank -> {
            wcPlayer.setRank(rank == null ? "Default" : rank);

            profileService.getPlayerTag(uuid, tag -> {
                wcPlayer.setTag(tag == null ? "" : tag);

                profileService.getChatColorPreference(uuid, color -> {
                    wcPlayer.setChatColorCode(color == null ? "&f" : color);

                    moderationService.isPlayerMuted(uuid, wcPlayer::setMuted);
                });
            });
        });
    }

    public void syncUpdate(UUID uuid) {
        String server = plugin.getConfig().getString("server-name", "Unknown");
        plugin.getRedisManager().publish(new PlayerUpdatePacket(server, System.currentTimeMillis(), uuid));
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) loadPlayerData(uuid, player.getName());
    }

    public WinterCorePlayer getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean isRestricted(Player player) {
        WinterCorePlayer data = cache.get(player.getUniqueId());
        if (data != null && data.isMuted()) {
            player.sendMessage(plugin.getMessageConfig().get("chat.muted",
                    "&cYou are muted and cannot send messages."));
            return true;
        }
        return false;
    }

    public void setChatColorPreference(Player player, String colorCode) {
        if (player == null || colorCode == null || colorCode.trim().isEmpty()) {
            return;
        }

        WinterCorePlayer data = cache.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new WinterCorePlayer(player.getUniqueId(), player.getName())
        );
        data.setChatColorCode(colorCode);

        profileService.setChatColorPreference(player.getUniqueId(), colorCode, () -> {
            if (plugin.getRedisManager() != null) {
                String server = plugin.getConfig().getString("server-name", "Unknown");
                plugin.getRedisManager().publish(new PlayerUpdatePacket(server, System.currentTimeMillis(), player.getUniqueId()));
            }
        });
        if (plugin.getRankManager() != null) {
            plugin.getRankManager().setMessageColorPreference(player, colorCode);
        }
    }

    public void sendReport(Player reporter, Player target, String reason) {
        if (reporter == null || target == null || reason == null || reason.trim().isEmpty()) return;

        String originServer = plugin.getConfig().getString("server-name", "Unknown");
        plugin.getRedisManager().publishAndHandleLocally(new PlayerReportPacket(
                originServer, System.currentTimeMillis(), reporter.getName(), target.getName(), reason, originServer
        ));
    }

    public void broadcastFreeze(Player target, Player staff, boolean isFrozen) {
        String alert = plugin.getMessageConfig().get(isFrozen
                        ? "moderation.freeze.staff-alert-frozen"
                        : "moderation.freeze.staff-alert-unfrozen",
                isFrozen
                        ? "&9[S] {target}&c has been frozen by {staff}."
                        : "&9[S] {target}&a has been unfrozen by {staff}.",
                "{target}", target.getDisplayName(),
                "{staff}", staff.getDisplayName());

        net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("wintercore.staff"))
                .forEach(p -> p.sendMessage(alert));
    }

    public String getIdentity(Player player) {
        DisguiseData data = plugin.getDisguiseRegistry().getDisguiseData(player.getUniqueId());
        if (data != null && data.getInfo() != null && data.getInfo().has("name")) {
            return data.getInfo().get("name").getAsString();
        }
        return player.getName();
    }
}
