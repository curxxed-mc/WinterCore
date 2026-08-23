package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
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
            send(player, "ping.self", "&bYour ping: {ping}",
                    "{ping}", getColoredPing(ping));
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target.isOnline()) {
                int ping = Utilities.getPing(target);

                send(player, "ping.other", "&b{target}'s ping: {ping}",
                        "{target}", target.getName(),
                        "{ping}", getColoredPing(ping));
            } else {
                send(player, "general.player-not-found", "&cPlayer not found.");
            }
        } else {
            sendUsage(player);
        }
    }

    private String getColoredPing(int ping) {
        String color;
        if (ping <= 50) {
            color = CC.translate("&a");
        } else if (ping <= 100) {
            color = CC.translate("&2");
        } else if (ping <= 150) {
            color = CC.translate("&e");
        } else if (ping <= 200) {
            color = CC.translate("&6");
        } else if (ping <= 300) {
            color = CC.translate("&c");
        } else {
            color = CC.translate("&4");
        }
        return color + ping + "ms" + CC.translate("&r");
    }
}
