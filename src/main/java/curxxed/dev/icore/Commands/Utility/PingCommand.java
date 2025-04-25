package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            int ping = NMSUtils.getPing(player);
            player.sendMessage(ChatColor.AQUA + "Your ping: " + getColoredPing(ping));
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                int ping = NMSUtils.getPing(target);
                player.sendMessage(ChatColor.AQUA + target.getName() + "'s ping: " + getColoredPing(ping));
            } else {
                player.sendMessage(ChatColor.RED + "Player not found.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /ping [player]");
        }

        return true;
    }

    private String getColoredPing(int ping) {
        ChatColor color;

        if (ping <= 50) {
            color = ChatColor.GREEN;
        } else if (ping <= 100) {
            color = ChatColor.DARK_GREEN;
        } else if (ping <= 150) {
            color = ChatColor.YELLOW;
        } else if (ping <= 200) {
            color = ChatColor.GOLD;
        } else if (ping <= 300) {
            color = ChatColor.RED;
        } else {
            color = ChatColor.DARK_RED;
        }

        return color + String.valueOf(ping) + "ms" + ChatColor.RESET;
    }
}
