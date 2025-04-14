package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.utils.BukkitReflection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {

    // Handle the /ping command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // If no argument is provided, ping the player themselves
            if (args.length == 0) {
                int ping = BukkitReflection.getPing(player);
                player.sendMessage(ChatColor.AQUA + "Your ping: " + ChatColor.RESET + ping + ChatColor.AQUA + "ms");

                // If one argument is provided, ping the specified player
            } else if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);

                if (target != null) {
                    int ping = BukkitReflection.getPing(target);
                    player.sendMessage(ChatColor.AQUA + target.getName() + "'s ping" + ": " + ChatColor.RESET + ping + ChatColor.AQUA + "ms");
                } else {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Usage: /ping [player]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
        }
        return true;
    }
}
