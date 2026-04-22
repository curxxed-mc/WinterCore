package net.curxxed.dev.wintercore.player;

import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerReportPacket;
import net.curxxed.dev.wintercore.database.redis.packet.packets.PlayerUpdatePacket;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.database.service.ProfileService;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            player.sendMessage(CC.translate("&cYou are muted and cannot send messages."));
            return true;
        }
        return false;
    }



    public void sendPrivateMessage(Player sender, Player recipient, String message) {
        WinterCorePlayer sData = cache.get(sender.getUniqueId());
        WinterCorePlayer rData = cache.get(recipient.getUniqueId());

        if (sData == null || rData == null) {
            sender.sendMessage(CC.translate("&cPlayer data is still loading..."));
            return;
        }

        ChatColor sColor = sData.getMessageColor();
        ChatColor rColor = rData.getMessageColor();

        String toPrefix = ChatColor.YELLOW + "(To " + rColor + recipient.getDisplayName() + ChatColor.YELLOW + ") ";
        String fromPrefix = ChatColor.YELLOW + "(From " + sColor + sender.getDisplayName() + ChatColor.YELLOW + ") ";

        sender.spigot().sendMessage(new TextComponent(toPrefix + message));
        recipient.spigot().sendMessage(new TextComponent(fromPrefix + message));
    }

    public void sendReport(Player reporter, Player target, String reason) {
        if (reporter == null || target == null || reason == null || reason.trim().isEmpty()) return;

        String originServer = plugin.getConfig().getString("server-name", "Unknown");
        plugin.getRedisManager().publishAndHandleLocally(new PlayerReportPacket(
                originServer, System.currentTimeMillis(), reporter.getName(), target.getName(), reason, originServer
        ));
    }

    public void broadcastFreeze(Player target, Player staff, boolean isFrozen) {
        String status = isFrozen ? "&c has been frozen by " : "&a has been unfrozen by ";
        String alert = CC.translate("&9[S] " + target.getDisplayName() + status + staff.getDisplayName() + ".");

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("wintercore.staff"))
                .forEach(p -> p.sendMessage(alert));
    }

    public String getIdentity(Player player) {
        DisguiseData data = plugin.getDisguiseDataMap().get(player.getUniqueId());
        if (data != null && data.getInfo() != null && data.getInfo().has("name")) {
            return data.getInfo().get("name").getAsString();
        }
        return player.getName();
    }
}
