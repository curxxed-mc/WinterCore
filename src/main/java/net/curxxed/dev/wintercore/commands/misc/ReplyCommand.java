package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

@CommandInfo(
        name = "reply",
        permission = "WinterCore.reply",
        description = "Reply to the last private message.",
        aliases = {"r", "respond"},
        usage = "/reply <message>",
        inGameOnly = true
)
public class ReplyCommand extends BaseCommand {

    private final WinterCore plugin;
    private final PlayerListener playerListener;

    private static final Map<UUID, LastMessageInfo> lastMessageMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long REPLY_TIMEOUT_MILLIS = 180_000; // 3 minutes

    public ReplyCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.playerListener = plugin.getPlayerListener();
    }

    public static void setLastSender(Player recipient, Player sender) {
        lastMessageMap.put(recipient.getUniqueId(), new LastMessageInfo(sender.getUniqueId(), System.currentTimeMillis()));
    }

    public static Player getReplyTarget(Player player) {
        LastMessageInfo info = lastMessageMap.get(player.getUniqueId());
        if (info == null) return null;
        if (System.currentTimeMillis() - info.timestamp > REPLY_TIMEOUT_MILLIS) {
            lastMessageMap.remove(player.getUniqueId());
            return null;
        }
        Player target = pluginInstance().getServer().getPlayer(info.sender);
        if (target == null || !target.isOnline()) {
            lastMessageMap.remove(player.getUniqueId());
            return null;
        }
        return target;
    }

    private static WinterCore pluginInstance() {
        return WinterCore.INSTANCE;
    }

    private static class LastMessageInfo {
        private final UUID sender;
        private final long timestamp;
        LastMessageInfo(UUID sender, long timestamp) {
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage("Only players can send private messages.");
            return;
        }
        Player lastSender = getReplyTarget(player);
        if (lastSender == null) {
            player.sendMessage(CC.translate("&cYou have no one to reply to."));
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length == 0) {
            player.sendMessage("Usage: /reply <message>");
            return;
        }
        String message = String.join(" ", args);
        playerListener.sendPrivateMessage(player, lastSender, message);
        setLastSender(lastSender, player); // allow reply chain
    }
}