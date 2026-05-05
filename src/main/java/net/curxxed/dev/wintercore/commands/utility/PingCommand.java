package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "ping",
        description = "Check your or another player's ping.",
        usage = "/ping [player]",
        inGameOnly = true,
        permission = {}
)
public class PingCommand extends BaseCommand {

    public PingCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length == 0) {
            int ping = Utilities.getPing(player);
            player.sendMessage(CC.AQUA + "Your ping: " + getColoredPing(ping));
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target.isOnline()) {
                int ping = Utilities.getPing(target);

                player.sendMessage(CC.AQUA + target.getName() + "'s ping: " + getColoredPing(ping));
            } else {
                player.sendMessage(CC.RED + "Player not found.");
            }
        } else {
            player.sendMessage(CC.RED + "Usage: /ping [player]");
        }
    }

    private String getColoredPing(int ping) {
        String color;
        if (ping <= 50) {
            color = CC.GREEN;
        } else if (ping <= 100) {
            color = CC.DARK_GREEN;
        } else if (ping <= 150) {
            color = CC.YELLOW;
        } else if (ping <= 200) {
            color = CC.GOLD;
        } else if (ping <= 300) {
            color = CC.RED;
        } else {
            color = CC.DARK_RED;
        }
        return color + ping + "ms" + CC.RESET;
    }
}
