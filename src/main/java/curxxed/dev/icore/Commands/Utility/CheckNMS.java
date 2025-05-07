package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckNMS  implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;

        String nmsType = NMSUtils.IS_LEGACY ? "Legacy NMS" : "Modern NMS";
        String serverVersion = NMSUtils.getServerVersion();
        p.sendMessage(ChatColor.AQUA + "Your server is using " + ChatColor.YELLOW + nmsType + ChatColor.AQUA + " and is running on " + ChatColor.YELLOW + serverVersion + ChatColor.AQUA + ".");
        return true;
    }
}
