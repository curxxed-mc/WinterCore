package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "sudo",
            description = "Forces a player to run a command or chat.",
            usage = "/sudo <player> <command|chat> <message>",
            permission = "wintercore.sudo",
            inGameOnly = false
    
    )
public class SudoCommand extends BaseCommand {

    public SudoCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() < 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /sudo <player> <command|chat> <message>");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase();
        if (action.equals("chat")) {
            if (commandArgs.length() < 3) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /sudo <player> chat <message>");
                return;
            }
            String message = String.join(" ", commandArgs.getArgs()).substring(
                    commandArgs.getArgs()[0].length() + commandArgs.getArgs()[0].length() + 2
            );
            target.chat(message);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to send a chat message.");
        } else {
            String commandToExecute = String.join(" ", commandArgs.getArgs()).substring(commandArgs.getArgs()[0].length() + 1);
            target.performCommand(commandToExecute);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to execute: /" + commandToExecute);
        }
    }
}