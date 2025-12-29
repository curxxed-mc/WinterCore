package net.curxxed.dev.wintercore.commands.misc;


import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;

@CommandInfo(
        name = "reloadconfig",
        permission = "WinterCore.reloadconfig",
        description = "Reloads the plugin configuration files.",
        usage = "/reloadconfig",
        inGameOnly = false
)
public class ReloadConfig extends BaseCommand {

    private final WinterCore plugin;

    public ReloadConfig(WinterCore pl) {
       super(pl);
       this.plugin = pl;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
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