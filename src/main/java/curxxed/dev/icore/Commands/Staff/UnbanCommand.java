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

public class UnbanCommand implements CommandExecutor {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public UnbanCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.unban")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to unban players.");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unban <player>");
            return false;
        }

        String targetName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();

        databaseManager.isPlayerBanned(targetUUID, isBanned -> {
            if (!isBanned) {
                sender.sendMessage(ChatColor.RED + "Player " + targetName + " is not banned.");
                return;
            }

            databaseManager.unbanPlayer(targetUUID);
            sender.sendMessage(ChatColor.GREEN + "Player " + targetName + " has been unbanned.");

            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You have been unbanned.");
            }
        });

        return true;
    }
}