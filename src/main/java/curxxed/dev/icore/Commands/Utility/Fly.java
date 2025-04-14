package curxxed.dev.icore.Commands.Utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Fly implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has the required permission
        if (!sender.hasPermission("iCore.fly")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) { // If no arguments, toggle flight for the sender
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can toggle their own flight.");
                return true;
            }

            Player player = (Player) sender;
            toggleFlight(player, player);
            return true;
        }

        // If a target player is specified, toggle their flight
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        toggleFlight(sender, target);
        return true;
    }

    private void toggleFlight(CommandSender sender, Player target) {
        target.setAllowFlight(!target.getAllowFlight());

        if (target.getAllowFlight()) {
            target.sendMessage(ChatColor.GREEN + "You can now fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + target.getName() + " can now fly!");
            }
        } else {
            target.sendMessage(ChatColor.RED + "You can no longer fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.RED + target.getName() + " can no longer fly!");
            }
        }
    }
}
