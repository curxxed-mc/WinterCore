package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.menus.ChatColorMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "chatcolor",
        description = "Open the chat colour selection menu.",
        usage = "/chatcolor",
        inGameOnly = true,
        permission = {"wintercore.commands.chatcolor", "WinterCore.commands.chatcolor"}
)
public class ChatColorCommand extends BaseCommand {

    public ChatColorCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        new ChatColorMenu(plugin, player).open(player);
    }
}