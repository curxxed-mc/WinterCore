package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "checknms",
        description = "Check the server's NMS type and version.",
        usage = "/checknms",
        inGameOnly = true,
        permission = {}
)
public class CheckNMS extends BaseCommand {

    public CheckNMS(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player p = commandArgs.getPlayer();

        String nmsType = Utilities.IS_LEGACY ? "Legacy NMS" : "Modern NMS";
        String serverVersion = Utilities.getServerVersion();
        p.sendMessage(CC.AQUA + "Your server is using " + CC.YELLOW + nmsType + CC.AQUA + " and is running on " + CC.YELLOW + serverVersion + CC.AQUA + ".");
    }
}




