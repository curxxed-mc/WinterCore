package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.UUID;

public class UnbanCommand extends BaseCommand {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public UnbanCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "unban",
        permission = "iCore.unban",
        description = "Unban a player.",
        usage = "/unban <player>",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("iCore.unban")) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "You do not have permission to unban players.");
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /unban <player>");
            return;
        }
        String targetName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        databaseManager.isPlayerBanned(targetUUID, isBanned -> {
            if (!isBanned) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Player " + targetName + " is not banned.");
                return;
            }
            databaseManager.unbanPlayer(targetUUID);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Player " + targetName + " has been unbanned.");
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You have been unbanned.");
            }
        });
    }
}

