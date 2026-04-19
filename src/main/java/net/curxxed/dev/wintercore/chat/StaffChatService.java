package net.curxxed.dev.wintercore.chat;

import net.curxxed.dev.wintercore.database.redis.packet.packets.ChatBroadcastPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class StaffChatService {

    private final WinterCore plugin;

    public StaffChatService(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void sendStaffMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.staff") && !player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use staff chat."));
            return;
        }
        buildAndPublish(player, message, "&9[SC] ", "&9", ChatBroadcastPacket.ChatType.STAFF);
    }

    public void sendAdminMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use admin chat."));
            return;
        }
        buildAndPublish(player, message, "&c[AC] ", "&c", ChatBroadcastPacket.ChatType.ADMIN);
    }

    public void sendManagerMessage(Player player, String message) {
        if (message.isEmpty()) return;
        if (!player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use manager chat."));
            return;
        }
        buildAndPublish(player, message, "&4[MC] ", "&4", ChatBroadcastPacket.ChatType.MANAGER);
    }

    private void buildAndPublish(Player player, String message, String prefix, String color, ChatBroadcastPacket.ChatType type) {
        plugin.getDisguiseRegistry().getEffectivePrefix(player, disguisePrefix -> {
            String formatted = CC.translate(prefix) + disguisePrefix + player.getDisplayName() + ": " + CC.translate(color) + message + ChatColor.RESET;
            plugin.getRedisManager().publish(new ChatBroadcastPacket(
                    plugin.getConfig().getString("server-name", "Unknown"),
                    System.currentTimeMillis(),
                    type,
                    formatted
            ));
        });
    }
}