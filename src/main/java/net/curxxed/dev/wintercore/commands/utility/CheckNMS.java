package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
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
        send(p, "check-nms.result",
                "&bYour server is using &e{nms_type}&b and is running on &e{server_version}&b.",
                "{nms_type}", nmsType,
                "{server_version}", serverVersion);
    }
}
