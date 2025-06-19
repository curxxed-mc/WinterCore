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

public class UnmuteCommand extends BaseCommand {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public UnmuteCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "unmute",
        permission = "iCore.unmute",
        description = "Unmute a player.",
        usage = "/unmute <player>",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("iCore.unmute")) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "You do not have permission to unmute players.");
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /unmute <player>");
            return;
        }
        String playerName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        databaseManager.isPlayerMuted(targetUUID, isMuted -> {
            if (!isMuted) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Player " + playerName + " is not muted.");
                return;
            }
            databaseManager.unmutePlayer(targetUUID);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have unmuted " + playerName + ".");
            Player target = Bukkit.getPlayerExact(playerName);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "You have been unmuted.");
            }
        });
    }
}

