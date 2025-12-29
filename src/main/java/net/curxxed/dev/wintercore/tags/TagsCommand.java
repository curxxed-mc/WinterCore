package net.curxxed.dev.wintercore.tags;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;
@CommandInfo(
        name = "tags",
        permission = "WinterCore.tags",
        description = "Open the tags GUI.",
        usage = "/tags",
        inGameOnly = true
)
public class TagsCommand extends BaseCommand {
    private final TagsGUI tagsGUI;

    public TagsCommand(TagsGUI tagsGUI, WinterCore plgugin) {
        super(plgugin);
        this.tagsGUI = tagsGUI;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage("Only players can use this command.");
            return;
        }
        tagsGUI.openTagsGUI(player);
    }
}

