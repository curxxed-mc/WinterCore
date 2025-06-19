package net.curxxed.dev.icore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PingCommand extends BaseCommand {

    @Command(
            name = "ping",
            description = "Check your or another player's ping.",
            usage = "/ping [player]",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length == 0) {
            int ping = NMSUtils.getPing(player);
            if (ping == -1) {
                player.sendMessage(NMSUtils.PING_ERROR);
            } else {
                player.sendMessage(ChatColor.AQUA + "Your ping: " + getColoredPing(ping));
            }
        } else if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null && target.isOnline()) {
                int ping = NMSUtils.getPing(target);
                if (ping == -1) {
                    player.sendMessage(NMSUtils.PING_ERROR);
                } else {
                    player.sendMessage(ChatColor.AQUA + target.getName() + "'s ping: " + getColoredPing(ping));
                }
            } else {
                player.sendMessage(ChatColor.RED + "Player not found.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /ping [player]");
        }
    }

    private String getColoredPing(int ping) {
        ChatColor color;
        if (ping <= 50) {
            color = ChatColor.GREEN;
        } else if (ping <= 100) {
            color = ChatColor.DARK_GREEN;
        } else if (ping <= 150) {
            color = ChatColor.YELLOW;
        } else if (ping <= 200) {
            color = ChatColor.GOLD;
        } else if (ping <= 300) {
            color = ChatColor.RED;
        } else {
            color = ChatColor.DARK_RED;
        }
        return color + String.valueOf(ping) + "ms" + ChatColor.RESET;
    }
}