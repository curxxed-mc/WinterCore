package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Heal extends BaseCommand {

    @Command(
            name = "heal",
            permission = "WinterCore.heal",
            description = "Heal yourself or another player.",
            usage = "/heal [player]",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        Player senderPlayer = commandArgs.getPlayer();
        Player target;

        if (commandArgs.length() > 0) {
            target = Bukkit.getPlayer(commandArgs.getArgs(0));
            if (target == null || !target.isOnline()) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found!");
                return;
            }
        } else if (commandArgs.isPlayer()) {
            target = senderPlayer;
        } else {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Only players can use this command on themselves!");
            return;
        }

        target.setHealth(20);
        target.sendMessage(ChatColor.GREEN + "Your health has been restored!");
        if (!target.equals(commandArgs.getSender())) {
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have healed " + target.getName() + "!");
        }
    }
}