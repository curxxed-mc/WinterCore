package net.curxxed.dev.wintercore.listeners;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.function.Consumer;

public class ChatListener implements Listener {

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final TagsManager tagsManager;
    private final PlayerListener playerListener;

    public ChatListener(WinterCore plugin, TagsManager tagsManager, PlayerListener playerListener) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.tagsManager = tagsManager;
        this.playerListener = playerListener;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String message = event.getMessage();

        playerListener.AreConditionsMet(player, isMuted -> {
            if (isMuted) return;

            if (message.startsWith("!")) { handleStaffChat(player, message.substring(1).trim()); return; }
            if (message.startsWith("@")) { handleAdminChat(player, message.substring(1).trim()); return; }
            if (message.startsWith("#")) { handleManagerChat(player, message.substring(1).trim()); return; }

            sendPublicChat(player, message);
        });
    }

    private void sendPublicChat(Player player, String message) {
        tagsManager.getPlayerTag(player.getUniqueId(), tag -> {
            String tagSuffix = tag != null
                    ? " " + CC.translate(TagsManager.colorNameToCode(tag.getColor())) + tag.getPrefix() + ChatColor.RESET
                    : "";

            plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix ->
                    plugin.getDisguiseRegistry().getEffectiveColor(player, color -> {
                        String colorCode = rankManager.getMessageColorPreference(player);
                        ChatColor messageColor = ChatColor.getByChar(colorCode.replace("&", "").charAt(0));

                        String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
                        String formattedName = formattedPrefix + CC.translate(color) + player.getName() + ChatColor.RESET + tagSuffix;
                        String formattedMessage = formattedName + ChatColor.WHITE + ": " + messageColor + message;

                        Bukkit.getConsoleSender().sendMessage(formattedMessage);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage(formattedMessage);
                        }
                    })
            );
        });
    }

    private void handleStaffChat(Player player, String content) {
        if (content.isEmpty()) return;
        if (!player.hasPermission("wintercore.staff") && !player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use staff chat."));
            return;
        }
        buildStaffChatMessage(player, content, msg -> plugin.getRedisManager().broadcastStaffMessage(msg));
    }

    private void handleAdminChat(Player player, String content) {
        if (content.isEmpty()) return;
        if (!player.hasPermission("wintercore.admin") && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use admin chat."));
            return;
        }
        buildAdminChatMessage(player, content, msg -> plugin.getRedisManager().broadcastAdminMessage(msg));
    }

    private void handleManagerChat(Player player, String content) {
        if (content.isEmpty()) return;
        if (!player.hasPermission("wintercore.manager")) {
            player.sendMessage(CC.translate("&cYou do not have permission to use manager chat."));
            return;
        }
        buildManagerChatMessage(player, content, msg -> plugin.getRedisManager().broadcastManagerMessage(msg));
    }

    public void buildStaffChatMessage(Player player, String message, Consumer<String> callback) {
        plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> {
            String formatted = CC.translate("&9[SC] ") + prefix + player.getDisplayName() + ": " + CC.translate("&9") + message + ChatColor.RESET;
            callback.accept(formatted);
        });
    }

    public void buildAdminChatMessage(Player player, String message, Consumer<String> callback) {
        plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> {
            String formatted = CC.translate("&c[AC] ") + prefix + player.getDisplayName() + ": " + CC.translate("&c") + message + ChatColor.RESET;
            callback.accept(formatted);
        });
    }

    public void buildManagerChatMessage(Player player, String message, Consumer<String> callback) {
        plugin.getDisguiseRegistry().getEffectivePrefix(player, prefix -> {
            String formatted = CC.translate("&4[MC] ") + prefix + player.getDisplayName() + ": " + CC.translate("&4") + message + ChatColor.RESET;
            callback.accept(formatted);
        });
    }
}