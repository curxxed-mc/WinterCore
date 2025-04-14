package curxxed.dev.icore.Commands.misc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class AboutCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public AboutCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String version = plugin.getDescription().getVersion();

        sender.sendMessage(ChatColor.GREEN + "========== " + ChatColor.AQUA + "About iCore" + ChatColor.GREEN + " ==========");
        sender.sendMessage(ChatColor.AQUA + "Plugin Name: " + ChatColor.WHITE + "iCore");
        sender.sendMessage(ChatColor.AQUA + "Version: " + ChatColor.WHITE + version);
        sender.sendMessage(ChatColor.AQUA + "Author: " + ChatColor.WHITE + "Curxxed");
        sender.sendMessage(ChatColor.AQUA + "Description: " + ChatColor.WHITE + "Core functionalities for the server, including ranks, staff tools, and more!");
        sender.sendMessage(ChatColor.AQUA + "Discord: "  + ChatColor.WHITE + "@curxxe");
        sender.sendMessage(ChatColor.GREEN + "=================================");
        return true;
    }
}
