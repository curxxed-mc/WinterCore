package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Feed extends BaseCommand {

    @Command(
            name = "feed",
            permission = "WinterCore.feed",
            description = "Feed yourself or another player.",
            usage = "/feed [player]",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (commandArgs.length() == 0) {
            if (!commandArgs.isPlayer()) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Only players can use this command on themselves!");
                return;
            }
            Player player = commandArgs.getPlayer();
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.sendMessage(ChatColor.GREEN + "You are now full.");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (target == null || !target.isOnline()) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.sendMessage(ChatColor.GREEN + "You have been fed by " + commandArgs.getSender().getName() + ".");
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have fed " + target.getName() + ".");
    }
}