package curxxed.dev.icore.Commands.misc;

import curxxed.dev.icore.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadConfig implements CommandExecutor {

    private final Main plugin;

    public ReloadConfig(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender has permission to reload the config
        if (!sender.hasPermission("iCore.reloadconfig")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return false;
        }

        // Reload the config file
        plugin.reloadConfig();

        // Send a confirmation message
        sender.sendMessage(ChatColor.GREEN + "Config file reloaded successfully!");

        return true;
    }
}
