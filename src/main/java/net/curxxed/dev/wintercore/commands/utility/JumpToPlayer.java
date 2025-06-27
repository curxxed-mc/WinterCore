package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class JumpToPlayer extends BaseCommand {

    @Command(
            name = "jtp",
            description = "Jump to another player.",
            usage = "/jtp <player>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player p = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length != 1) {
            p.sendMessage(ChatColor.RED + "Usage: /jtp <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        p.teleport(target.getLocation());
        p.sendMessage(ChatColor.GREEN + "Jumped to " + target.getName() + ".");
    }
}