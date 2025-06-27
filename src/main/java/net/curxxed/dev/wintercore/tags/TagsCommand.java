package net.curxxed.dev.wintercore.tags;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.entity.Player;

public class TagsCommand extends BaseCommand {
    private final TagsGUI tagsGUI;

    public TagsCommand(TagsGUI tagsGUI) {
        this.tagsGUI = tagsGUI;
    }

    @Command(
        name = "tags",
        permission = "WinterCore.tags",
        description = "Open the tags GUI.",
        usage = "/tags",
        inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage("Only players can use this command.");
            return;
        }
        tagsGUI.openTagsGUI(player);
    }
}

