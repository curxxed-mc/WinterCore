package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.GUI.StaffListGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffListCommand implements CommandExecutor {

    private final Main plugin;

    public StaffListCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (!sender.hasPermission("icore.stafflist") || !sender.hasPermission("icore.staff") || !sender.hasPermission("icore.admin") || !sender.hasPermission("icore.manager")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;

        StaffListGUI var_23u2h4534uibf = new StaffListGUI(plugin);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> var_23u2h4534uibf.openGUI(player, plugin.getRedisManager()));

        return true;
    }
}
