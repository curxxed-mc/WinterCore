package hyp.ilfov.i.icore.Commands.Gamemode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class gmc implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("iCore.gamemode")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        Player target;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Only players can use this command on themselves!");
            return true;
        }

        target.setGameMode(GameMode.CREATIVE);
        target.sendMessage(ChatColor.AQUA + "Your game mode has been set to Creative Mode.");
        if (!target.equals(sender)) {
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s game mode to Creative.");
        }
        return true;
    }
}
