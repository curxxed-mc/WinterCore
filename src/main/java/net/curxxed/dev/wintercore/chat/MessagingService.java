package net.curxxed.dev.wintercore.chat;

import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.player.WinterCorePlayer;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessagingService {

    private static final long REPLY_TIMEOUT_MILLIS = 180_000L;

    private final WinterCore plugin;
    private final PlayerService playerService;
    private final Map<UUID, LastMessageInfo> lastMessageMap = new ConcurrentHashMap<>();

    public MessagingService(WinterCore plugin, PlayerService playerService) {
        this.plugin = plugin;
        this.playerService = playerService;
    }

    public boolean sendPrivateMessage(Player sender, Player recipient, String message) {
        if (sender == null || recipient == null) {
            return false;
        }

        if (sender.equals(recipient)) {
            sender.sendMessage(plugin.getMessageConfig().get("chat.private.self",
                    "&cYou cannot send a message to yourself."));
            return false;
        }

        String trimmedMessage = message == null ? "" : message.trim();
        if (trimmedMessage.isEmpty()) {
            sender.sendMessage(plugin.getMessageConfig().get("chat.private.empty",
                    "&cMessage cannot be empty."));
            return false;
        }

        if (playerService.isRestricted(sender)) {
            return false;
        }

        if (plugin.getChatFilterService() != null
                && plugin.getChatFilterService().checkAndNotify(sender, trimmedMessage, ChatFilterService.MessageChannel.PRIVATE_MESSAGE)) {
            return false;
        }

        WinterCorePlayer senderData = playerService.getPlayerData(sender.getUniqueId());
        WinterCorePlayer recipientData = playerService.getPlayerData(recipient.getUniqueId());
        if (senderData == null || recipientData == null) {
            sender.sendMessage(plugin.getMessageConfig().get("chat.private.data-loading",
                    "&cPlayer data is still loading..."));
            return false;
        }

        String senderColor = senderData.getMessageColor();
        String recipientColor = recipientData.getMessageColor();

        String toMessage = formatPrivateMessage("chat.private.to",
                "&e(To {recipient}&e) &f{message}",
                "{recipient}", recipientColor + recipient.getDisplayName());
        String fromMessage = formatPrivateMessage("chat.private.from",
                "&e(From {sender}&e) &f{message}",
                "{sender}", senderColor + sender.getDisplayName());

        sender.spigot().sendMessage(new TextComponent(toMessage.replace("{message}", trimmedMessage)));
        recipient.spigot().sendMessage(new TextComponent(fromMessage.replace("{message}", trimmedMessage)));

        setLastSender(recipient, sender);
        setLastSender(sender, recipient);
        return true;
    }

    public void setLastSender(Player recipient, Player sender) {
        if (recipient == null || sender == null) {
            return;
        }
        lastMessageMap.put(recipient.getUniqueId(), new LastMessageInfo(sender.getUniqueId(), System.currentTimeMillis()));
    }

    public Player getReplyTarget(Player player) {
        if (player == null) {
            return null;
        }

        UUID playerId = player.getUniqueId();
        LastMessageInfo info = lastMessageMap.get(playerId);
        if (info == null) {
            return null;
        }

        if (System.currentTimeMillis() - info.timestamp > REPLY_TIMEOUT_MILLIS) {
            lastMessageMap.remove(playerId);
            return null;
        }

        Player target = plugin.getServer().getPlayer(info.sender);
        if (target == null || !target.isOnline()) {
            lastMessageMap.remove(playerId);
            return null;
        }
        return target;
    }

    private static final class LastMessageInfo {
        private final UUID sender;
        private final long timestamp;

        private LastMessageInfo(UUID sender, long timestamp) {
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }

    private String formatPrivateMessage(String path, String fallback, String... placeholders) {
        String[] withMessageToken = new String[placeholders.length + 2];
        System.arraycopy(placeholders, 0, withMessageToken, 0, placeholders.length);
        withMessageToken[placeholders.length] = "{message}";
        withMessageToken[placeholders.length + 1] = "{message}";
        return plugin.getMessageConfig().get(path, fallback, withMessageToken);
    }
}
