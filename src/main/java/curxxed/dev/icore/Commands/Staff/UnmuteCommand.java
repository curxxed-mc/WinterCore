package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public UnmuteCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.unmute")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to unmute players.");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player>");
            return false;
        }

        String playerName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();

        databaseManager.isPlayerMuted(targetUUID, isMuted -> {
            if (!isMuted) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not muted.");
                return;
            }

            databaseManager.unmutePlayer(targetUUID);
            sender.sendMessage(ChatColor.GREEN + "You have unmuted " + playerName + ".");

            Player target = Bukkit.getPlayerExact(playerName);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You have been unmuted.");
            }
        });

        return true;
    }
}