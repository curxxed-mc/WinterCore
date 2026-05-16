package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "reply",
        description = "Reply to the last private message.",
        aliases = {"r", "respond"},
        usage = "/reply <message>",
        inGameOnly = true,
        permission = {"wintercore.reply", "WinterCore.reply"}
)
public class ReplyCommand extends BaseCommand {

    private final MessagingService messagingService;

    public ReplyCommand(WinterCore plugin) {
        super(plugin);
        this.messagingService = plugin.getMessagingService();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            send(commandArgs.getSender(), "chat.private.player-only",
                    "&cOnly players can send private messages.");
            return;
        }

        if (commandArgs.length() < 1) {
            sendUsage(player);
            return;
        }

        if (messagingService == null) {
            send(player, "general.internal-error",
                    "&cAn internal error occurred. Please contact an administrator.");
            plugin.getLogger().severe("ReplyCommand could not execute because MessagingService was not found!");
            return;
        }

        Player replyTarget = messagingService.getReplyTarget(player);
        if (replyTarget == null) {
            player.sendMessage(plugin.getMessageConfig().get("chat.private.no-reply-target",
                    "&cYou have no one to reply to."));
            return;
        }

        String message = String.join(" ", commandArgs.getArgs());
        messagingService.sendPrivateMessage(player, replyTarget, message);
    }
}
