package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
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