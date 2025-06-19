/*package net.curxxed.dev.icore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.listeners.PlayerListener;
import org.bukkit.entity.Player;

public class ReplyCommand extends BaseCommand {

    private final iCore plugin;
    private final PlayerListener playerListener;

    public ReplyCommand(iCore plugin) {
        this.plugin = plugin;
        this.playerListener = plugin.getPlayerListener();
    }

    @Command(
        name = "r",
        permission = "iCore.reply",
        description = "Reply to the last private message.",
        usage = "/r <message>",
        inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage("Only players can send private messages.");
            return;
        }
        if (!plugin.getPlayerDataManager().hasLastSender(player)) {
            player.sendMessage("You have no one to reply to.");
            return;
        }
        Player lastSender = plugin.getPlayerDataManager().getLastSender(player);
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            player.sendMessage("Usage: /r <message>");
            return;
        }
        String message = String.join(" ", args);
        playerListener.sendPrivateMessage(player, lastSender, message);
    }
}
/*
 */
