package net.curxxed.dev.wintercore.chat;

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

    private final WinterCore plugin;
    private final RankManager rankManager;
    private final TagsManager tagsManager;
    private final PlayerService playerService;
    private final StaffChatService service;

    public ChatListener(WinterCore plugin, TagsManager tagsManager, PlayerService playerService, StaffChatService service) {
        this.plugin = plugin;
        this.rankManager = RankManager.getInstance();
        this.tagsManager = tagsManager;
        this.playerService = playerService;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handleChat(player, message));
    }

    private void handleChat(Player player, String message) {
        if (playerService.isRestricted(player)) {
            return;
        }

        ChatFilterService.MessageChannel channel = isStaffChannel(message)
                ? ChatFilterService.MessageChannel.STAFF_CHAT
                : ChatFilterService.MessageChannel.PUBLIC_CHAT;
        if (plugin.getChatFilterService() != null
                && plugin.getChatFilterService().checkAndNotify(player, message, channel)) {
            return;
        }

        if (message.startsWith("!")) {
            String content = message.substring(1).trim();
            if (!content.isEmpty()) service.sendStaffMessage(player, content);
            return;
        }
        if (message.startsWith("@")) {
            String content = message.substring(1).trim();
            if (!content.isEmpty()) service.sendAdminMessage(player, content);
            return;
        }
        if (message.startsWith("#")) {
            String content = message.substring(1).trim();
            if (!content.isEmpty()) service.sendManagerMessage(player, content);
            return;
        }

        sendPublicChat(player, message);
    }

    private boolean isStaffChannel(String message) {
        return message.startsWith("!") || message.startsWith("@") || message.startsWith("#");
    }

    private void sendPublicChat(Player player, String message) {
        WinterCorePlayer data = playerService.getPlayerData(player.getUniqueId());

        if (data == null) {
            player.sendMessage(plugin.getMessageConfig().get("general.player-data-loading",
                    "&cYour data is still loading... Please wait a moment."));
            return;
        }
        String effectiveRank = rankManager.getDisguiseRankSync(player);
        if (effectiveRank == null) {
            effectiveRank = data.getRank();
        }

        String prefix = rankManager.getConfigManager().getPrefix(effectiveRank);
        String nameColor = rankManager.getDisguiseColorPreferenceSync(player);

        String tagId = data.getTag();
        String tagSuffix = "";
        if (tagId != null && !tagId.isEmpty()) {
            Tag tag = tagsManager.getTag(tagId);
            if (tag != null) {
                tagSuffix = " " + CC.translate(TagsManager.colorNameToCode(tag.getColor())) + tag.getPrefix() + CC.translate("&r");
            }
        }

        String messageColor = data.getMessageColor();
        String formattedPrefix = prefix.isEmpty() ? "" : prefix + " ";
        String formattedName = formattedPrefix + CC.translate(nameColor) + player.getName() + CC.translate("&r") + tagSuffix;
        String formattedMessage = plugin.getMessageConfig().get("chat.public-format",
                "{name}&f: {message_color}{message}",
                "{name}", formattedName,
                "{message_color}", messageColor,
                "{message}", message);
        Bukkit.getConsoleSender().sendMessage(formattedMessage);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(formattedMessage);
        }
    }
}
