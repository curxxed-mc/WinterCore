package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SudoCommand extends BaseCommand {

    @Command(
            name = "sudo",
            description = "Force a player to run a command or chat.",
            usage = "/sudo <player> <command|chat> <message>",
            permission = "icore.sudo",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (commandArgs.length() < 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /sudo <player> <command|chat> <message>");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (target == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs(1).toLowerCase();
        if (action.equals("chat")) {
            if (commandArgs.length() < 3) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /sudo <player> chat <message>");
                return;
            }
            String message = String.join(" ", commandArgs.getArgs()).substring(
                    commandArgs.getArgs(0).length() + commandArgs.getArgs(1).length() + 2
            );
            target.chat(message);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to send a chat message.");
        } else {
            String commandToExecute = String.join(" ", commandArgs.getArgs()).substring(commandArgs.getArgs(0).length() + 1);
            target.performCommand(commandToExecute);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to execute: /" + commandToExecute);
        }
    }
}