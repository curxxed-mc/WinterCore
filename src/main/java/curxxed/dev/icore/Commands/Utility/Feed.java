package curxxed.dev.icore.Commands.Utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Feed implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if the sender has permission
        if (!sender.hasPermission("iCore.feed")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) { // If no arguments are provided, feed the sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command on themselves!");
                return true;
            }

            Player player = (Player) sender;
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.sendMessage(ChatColor.GREEN + "You are now full.");
            return true;
        }

        // If an argument is provided, attempt to feed the specified player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        // Feed the target player
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.sendMessage(ChatColor.GREEN + "You have been fed by " + sender.getName() + ".");
        sender.sendMessage(ChatColor.GREEN + "You have fed " + target.getName() + ".");

        return true;
    }
}
