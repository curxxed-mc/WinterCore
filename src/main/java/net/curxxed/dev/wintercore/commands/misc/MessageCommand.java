package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class MessageCommand extends BaseCommand {

    private final WinterCore plugin;
    private final PlayerListener playerListener;

    public MessageCommand(WinterCore plugin) {
        this.plugin = plugin;
        this.playerListener = plugin.getPlayerListener();
    }

    @Command(
            name = "msg",
            aliases = {"message", "tell", "whisper", "w", "m"},
            description = "Send private message to another player.",
            usage = "/msg <player> <message>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /msg <player> <message>");
            return;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null || !recipient.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (recipient.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot message yourself.");
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        playerListener.sendPrivateMessage(player, recipient, message);
        // Register last sender for /reply functionality
        ReplyCommand.setLastSender(recipient, player);
    }
}