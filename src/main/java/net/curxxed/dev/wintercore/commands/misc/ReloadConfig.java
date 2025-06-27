package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;

public class ReloadConfig extends BaseCommand {

    private final WinterCore plugin;

    public ReloadConfig(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "reloadconfig",
            permission = "WinterCore.reloadconfig",
            description = "Reloads the plugin configuration files.",
            usage = "/reloadconfig",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("WinterCore.reloadconfig")) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        plugin.reloadConfig();

        if (plugin.getTagsManager() != null) {
            plugin.getTagsManager().loadTags();
            plugin.getTagsGUI().refresh();
        }

        if (plugin.getRankManager() != null) {
            plugin.getRankManager().reloadRanksConfig();
        }

        commandArgs.getSender().sendMessage(
                ChatColor.AQUA + "Sexy files have been reloaded: " +
                        ChatColor.GRAY + "[" +
                        ChatColor.AQUA + "ranks.yml" +
                        ChatColor.RESET + ", " +
                        ChatColor.AQUA + "config.yml" +
                        ChatColor.GRAY + ", " +
                        ChatColor.AQUA + "tags.yml" +
                        ChatColor.GRAY + "]"
        );
    }
}