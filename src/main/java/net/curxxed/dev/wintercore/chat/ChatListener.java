package net.curxxed.dev.wintercore.chat;

import lombok.Getter;
import lombok.Setter;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.player.WinterCorePlayer;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.tags.Tag;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private static final char STAFF_PREFIX = '!';
    private static final char ADMIN_PREFIX = '@';
    private static final char MANAGER_PREFIX = '#';

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final TagsManager tagsManager;
    private final PlayerService playerService;
    private final StaffChatService service;

    @Setter
    @Getter
    private boolean chatMuted = false;

    public ChatListener(WinterCore plugin, TagsManager tagsManager, PlayerService playerService, StaffChatService service) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.tagsManager = tagsManager;
        this.playerService = playerService;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String message = event.getMessage();

        plugin.getTasks().sync(() -> handleChat(player, message));
    }

    private void handleChat(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        if (playerService.isRestricted(player)) {
            return;
        }

        if (chatMuted && !player.hasPermission("servermute")) {
            player.sendMessage(plugin.getMessageConfig().get(
                    "server.chat.muted",
                    "&CChat is Currently muted."
            ));
            return;
        }

        ChatFilterService filterService = plugin.getChatFilterService();
        ChatFilterService.MessageChannel channel = getMessageChannel(message);
        if (filterService != null && filterService.checkAndNotify(player, message, channel)) {
            return;
        }

        if (message.isEmpty()) {
            return;
        }

        char prefix = message.charAt(0);
        String content = message.substring(1).trim();

        if (prefix == STAFF_PREFIX) {
            if (!content.isEmpty()) {
                service.sendStaffMessage(player, content);
            }
            return;
        }

        if (prefix == ADMIN_PREFIX) {
            if (!content.isEmpty()) {
                service.sendAdminMessage(player, content);
            }
            return;
        }

        if (prefix == MANAGER_PREFIX) {
            if (!content.isEmpty()) {
                service.sendManagerMessage(player, content);
            }
            return;
        }

        sendPublicChat(player, message);
    }

    private ChatFilterService.MessageChannel getMessageChannel(String message) {
        if (message == null || message.isEmpty()) {
            return ChatFilterService.MessageChannel.PUBLIC_CHAT;
        }

        char prefix = message.charAt(0);
        if (prefix == STAFF_PREFIX || prefix == ADMIN_PREFIX || prefix == MANAGER_PREFIX) {
            return ChatFilterService.MessageChannel.STAFF_CHAT;
        }

        return ChatFilterService.MessageChannel.PUBLIC_CHAT;
    }

    private void sendPublicChat(Player player, String message) {
        WinterCorePlayer data = playerService.getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(plugin.getMessageConfig().get(
                    "general.player-data-loading",
                    "&cYour data is still loading... Please wait a moment."
            ));
            return;
        }

        String effectiveRank = rankManager.getDisguiseRankSync(player);
        if (effectiveRank == null) {
            effectiveRank = data.getRank();
        }

        String prefix = rankManager.getConfigManager().getPrefix(effectiveRank);
        String nameColor = rankManager.getDisguiseColorPreferenceSync(player);

        String tagSuffix = "";
        String tagId = data.getTag();
        if (tagId != null && !tagId.isEmpty()) {
            Tag tag = tagsManager.getTag(tagId);
            if (tag != null) {
                tagSuffix = " " + CC.translate(TagsManager.colorNameToCode(tag.getColor())) + tag.getPrefix() + CC.translate("&r");
            }
        }

        String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
        String formattedName = formattedPrefix + CC.translate(nameColor) + player.getName() + CC.translate("&r") + tagSuffix;
        String formattedMessage = plugin.getMessageConfig().get(
                "chat.public-format",
                "{name}&f: {message_color}{message}",
                "{name}", formattedName,
                "{message_color}", data.getMessageColor(),
                "{message}", message
        );

        Bukkit.getConsoleSender().sendMessage(formattedMessage);
        for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
            online.sendMessage(formattedMessage);
        }
    }
}
