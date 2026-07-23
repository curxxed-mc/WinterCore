package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

@CommandInfo(
        name = "message",
        aliases = {"msg", "tell", "whisper", "w", "m"},
        description = "Sends a private message to another player.",
        usage = "/message <player> <message>",
        inGameOnly = true,
        permission = {"wintercore.command.message"}
)
public class MessageCommand extends BaseCommand {

    private final MessagingService messagingService;

    public MessageCommand(WinterCore plugin) {
        super(plugin);
        this.messagingService = plugin.getMessagingService();
    }

    @Override
    public void execute(CommandArguments args) {
        Player sender = (Player) args.getSender();
        if (args.length() < 2) {
            sendUsage(sender);
            return;
        }
        Player target = args.getOptionalPlayer(0).orElse(null);
        if (target == null) {
            send(sender, "chat.private.target-not-online",
                    "&cThat player is not online.");
            return;
        }
        if (sender.equals(target)) {
            send(sender, "chat.private.self",
                    "&cYou cannot send a message to yourself.");
            return;
        }
        String message = args.getArgsList().stream()
                .skip(1)
                .collect(Collectors.joining(" "));

        if (this.messagingService != null) {
            this.messagingService.sendPrivateMessage(sender, target, message);
        } else {
            send(sender, "general.internal-error",
                    "&cAn internal error occurred. Please contact an administrator.");
            plugin.getLogger().severe("MessageCommand could not execute because MessagingService was not found!");
        }
    }
}
