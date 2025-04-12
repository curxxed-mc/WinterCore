package hyp.ilfov.i.icore.Commands.Utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JumpToPlayer implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length != 1) {
            p.sendMessage(ChatColor.RED + "Usage: /jtp <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        p.teleport(target.getLocation());
        p.sendMessage(ChatColor.GREEN + "Jumped to " + target.getName() + ".");
        return true;
    }
}
