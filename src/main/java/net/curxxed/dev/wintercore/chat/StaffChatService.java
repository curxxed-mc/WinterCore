package net.curxxed.dev.wintercore.chat;

import net.curxxed.dev.wintercore.database.redis.packet.packets.ChatBroadcastPacket;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class StaffChatService {

    private final WinterCore plugin;

    public StaffChatService(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void sendStaffMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.staff") && !player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(message("staff-chat.no-permission.staff", "&cYou do not have permission to use staff chat."));
            return;
        }
        buildAndPublish(player, message, "staff-chat.format.staff",
                "&9[SC] &8[&7{server}&8] {identity}&r: &9{message}&r", ChatBroadcastPacket.ChatType.STAFF);
    }

    public void sendAdminMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(message("staff-chat.no-permission.admin", "&cYou do not have permission to use admin chat."));
            return;
        }
        buildAndPublish(player, message, "staff-chat.format.admin",
                "&c[AC] &8[&7{server}&8] {identity}&r: &c{message}&r", ChatBroadcastPacket.ChatType.ADMIN);
    }

    public void sendManagerMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.manager")) {
            player.sendMessage(message("staff-chat.no-permission.manager", "&cYou do not have permission to use manager chat."));
            return;
        }
        buildAndPublish(player, message, "staff-chat.format.manager",
                "&4[MC] &8[&7{server}&8] {identity}&r: &4{message}&r", ChatBroadcastPacket.ChatType.MANAGER);
    }

    private void buildAndPublish(Player player, String chatMessage, String path, String fallback, ChatBroadcastPacket.ChatType type) {
        final String sourceServer = plugin.getConfig().getString("server-name", "Unknown");
        final String realName = resolveRealName(player);

        plugin.getRankManager().getRank(player.getUniqueId(), rank -> {
            String rawRankPrefix = rank != null ? plugin.getRankManager().getRankPrefixSync(rank) : "";
            String translatedRankPrefix = rawRankPrefix != null ? CC.translate(rawRankPrefix) : "";
            plugin.getRankManager().getColorPreference(rank, nameColor -> {
                String identity = formatIdentity(translatedRankPrefix, nameColor, realName);

                String formatted = message(path, fallback,
                        "{server}", sourceServer,
                        "{identity}", identity,
                        "{message}", chatMessage);

                plugin.getRedisManager().publishAndHandleLocally(new ChatBroadcastPacket(
                        sourceServer,
                        System.currentTimeMillis(),
                        type,
                        formatted
                ));
            });
        });
    }

    private String formatIdentity(String translatedRankPrefix, String nameColor, String realName) {
        String translatedNameColor = CC.translate(
                (nameColor == null || nameColor.trim().isEmpty()) ? "&f" : nameColor
        );
        String coloredName = translatedNameColor + realName;

        if (translatedRankPrefix == null) {
            return coloredName;
        }

        String visible = CC.stripColor(translatedRankPrefix);
        if (visible == null || visible.trim().isEmpty()) {
            return coloredName;
        }

        if (translatedRankPrefix.endsWith(" ")) {
            return translatedRankPrefix + coloredName;
        }
        return translatedRankPrefix + " " + coloredName;
    }

    private String resolveRealName(Player player) {
        UUID uuid = player.getUniqueId();

        DisguiseData data = plugin.getDisguiseDataMap().get(uuid);
        if (data != null && data.getInfo() != null && data.getInfo().has("name") && !data.getInfo().get("name").isJsonNull()) {
            try {
                String original = data.getInfo().get("name").getAsString();
                if (original != null && !original.trim().isEmpty()) {
                    return original;
                }
            } catch (Exception ignored) {
            }
        }

        try {
            String cached = plugin.getNRS().getCachedUsername(uuid.toString());
            if (cached != null && !cached.trim().isEmpty()) {
                return cached;
            }
        } catch (Exception ignored) {
        }

        return player.getName();
    }

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }
}



