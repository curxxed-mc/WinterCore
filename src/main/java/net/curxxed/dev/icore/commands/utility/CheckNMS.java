package net.curxxed.dev.icore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CheckNMS extends BaseCommand {

    @Command(
            name = "checknms",
            description = "Check the server's NMS type and version.",
            usage = "/checknms",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player p = commandArgs.getPlayer();

        String nmsType = NMSUtils.IS_LEGACY ? "Legacy NMS" : "Modern NMS";
        String serverVersion = NMSUtils.getServerVersion();
        p.sendMessage(ChatColor.AQUA + "Your server is using " + ChatColor.YELLOW + nmsType + ChatColor.AQUA + " and is running on " + ChatColor.YELLOW + serverVersion + ChatColor.AQUA + ".");
    }
}