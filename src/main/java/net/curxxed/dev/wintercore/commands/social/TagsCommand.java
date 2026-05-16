package net.curxxed.dev.wintercore.commands.social;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.menus.TagsMenu;
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
    private final TagsMenu tagsMenu;

    public TagsCommand(TagsMenu tagsMenu, WinterCore plgugin) {
        super(plgugin);
        this.tagsMenu = tagsMenu;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            send(commandArgs.getSender(), "general.in-game-only-command", "&cOnly players can use this command.");
            return;
        }
        tagsMenu.openTagsGUI(player);
    }
}

