package net.curxxed.dev.icore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class MessageCommand extends BaseCommand {

    private final iCore plugin;
    private final PlayerListener playerListener;

    public MessageCommand(iCore plugin) {
        this.plugin = plugin;
        this.playerListener = plugin.getPlayerListener();
    }

    @Command(
            name = "msg",
            aliases = {"message", "tell", "whisper", "w", "m"},
            description = "Send a private message to another player.",
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
    }
}