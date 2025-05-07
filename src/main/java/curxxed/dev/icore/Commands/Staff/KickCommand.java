package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class KickCommand implements CommandExecutor {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public KickCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("iCore.kick")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to kick players.");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Please specify a player to kick.");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";
        String playerName = args[0];

        // Increment the kick count in the database
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        databaseManager.incrementKickCount(targetUUID);

        // Kick the player
        target.kickPlayer(ChatColor.RED + "You have been kicked for: " + reason);

        // Notify the sender
        player.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been kicked for: " + reason);

        return true;
    }
}