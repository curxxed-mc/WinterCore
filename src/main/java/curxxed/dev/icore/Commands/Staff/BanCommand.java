package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.PunishmentManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanCommand implements CommandExecutor {
    private final Main plugin;

    public BanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the sender is a player and has permission to ban
        Player player = (Player) sender;

        if (!player.hasPermission("iCore.ban")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to ban players.");
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Please specify a player to ban.");
            return false;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return false;
        }

        // Check if the target is already banned
        PunishmentManager punishmentManager = new PunishmentManager(plugin);
        if (punishmentManager.isPlayerBanned(targetName)) {
            player.sendMessage(ChatColor.RED + "This player is already banned.");
            return false;
        }

        // Add the ban to the punishment data (permanent ban)
        punishmentManager.addPunishment(targetName, "ban", "Banned by " + player.getName(), player.getName(), null); // null means permanent ban

        // Ban the player and kick them
        target.kickPlayer(ChatColor.RED + "You have been banned by " + player.getName() + ". You will not be able to rejoin.");
        plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, "Banned by " + player.getName(), null, null);

        player.sendMessage(ChatColor.GREEN + "Player " + targetName + " has been permanently banned.");

        return true;
    }
}
