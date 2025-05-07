package curxxed.dev.icore.Commands.Staff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SudoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("icore.sudo")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sudo <player> <command|chat> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }


        String action = args[1].toLowerCase();
        if (action.equals("chat")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /sudo <player> chat <message>");
                return true;
            }
            String message = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
            target.chat(message);
            sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to send a chat message.");
        } else {
            String commandToExecute = String.join(" ", args).substring(args[0].length() + 1);
            target.performCommand(commandToExecute);
            sender.sendMessage(ChatColor.GREEN + "Forced " + target.getName() + " to execute: /" + commandToExecute);
        }

        return true;
    }
}