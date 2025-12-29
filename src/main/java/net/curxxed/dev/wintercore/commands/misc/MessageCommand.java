package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

@CommandInfo(
        name = "message",
        aliases = {"msg", "tell", "whisper", "w", "m"},
        permission = "wintercore.command.message",
        description = "Sends a private message to another player.",
        usage = "/message <player> <message>",
        inGameOnly = true // This command can only be executed by a player
)
public class MessageCommand extends BaseCommand {

    private final PlayerListener playerListener;

    public MessageCommand(WinterCore plugin) {
        super(plugin);
        // Get the singleton instance of PlayerListener from the main plugin class
        this.playerListener = plugin.getPlayerListener();
    }

    @Override
    public void execute(CommandArguments args) {
        Player sender = (Player) args.getSender();

        // Ensure the command has enough arguments (player and message)
        if (args.length() < 2) {
            sender.sendMessage(CC.translate("&cUsage: " + commandInfo.usage()));
            return;
        }

        // Safely get the target player from the first argument
        Player target = args.getOptionalPlayer(0).orElse(null);

        // Check if the target player is online
        if (target == null) {
            sender.sendMessage(CC.translate("&cThat player is not online."));
            return;
        }

        // Prevent a player from messaging themselves
        if (sender.equals(target)) {
            sender.sendMessage(CC.translate("&cYou cannot send a message to yourself."));
            return;
        }

        // Rebuild the message from the remaining arguments
        String message = args.getArgsList().stream()
                .skip(1) // Skip the player name argument
                .collect(Collectors.joining(" "));

        // Delegate the actual message sending to the PlayerListener
        // This keeps the command class clean and respects your existing logic for /reply
        if (this.playerListener != null) {
            this.playerListener.sendPrivateMessage(sender, target, message);
        } else {
            // This is a safeguard in case the listener isn't initialized correctly
            sender.sendMessage(CC.translate("&cAn internal error occurred. Please contact an administrator."));
            plugin.getLogger().severe("MessageCommand could not execute because PlayerListener was not found!");
        }
    }
}
