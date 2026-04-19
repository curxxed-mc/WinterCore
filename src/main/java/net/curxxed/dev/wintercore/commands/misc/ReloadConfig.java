package net.curxxed.dev.wintercore.commands.misc;


import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;

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
        plugin.reloadConfig();

        if (plugin.getTagsManager() != null) {
            plugin.getTagsManager().loadTags();
            plugin.getTagsGUI().refresh();
        }

        if (plugin.getRankManager() != null) {
            plugin.getRankManager().reloadRanksConfig();
        }

        commandArgs.getSender().sendMessage(
                CC.translate("&bSexy files have been reloaded: &7[&branks.yml&r, &bconfig.yml&7, &btags.yml&7]")
        );
    }
}