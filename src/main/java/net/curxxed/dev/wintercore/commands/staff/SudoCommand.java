package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandInfo(
        name = "sudo",
        description = "Forces a player to run a command or chat.",
        usage = "/sudo <player> <command|chat> <message>",
        inGameOnly = false,
        permission = {"wintercore.sudo"}
)
public class SudoCommand extends BaseCommand {

    public SudoCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() < 3) {
            sendUsage(commandArgs.getSender());
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null) {
            send(commandArgs.getSender(), "general.player-not-found",
                    "&cPlayer not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase();
        if (action.equals("chat")) {
            String message = String.join(" ", Arrays.copyOfRange(commandArgs.getArgs(), 2, commandArgs.length()));
            target.chat(message);
            send(commandArgs.getSender(), "sudo.chat-success",
                    "&aForced {target} to send a chat message.",
                    "{target}", target.getName());
            return;
        }

        if (action.equals("command")) {
            String commandToExecute = String.join(" ", Arrays.copyOfRange(commandArgs.getArgs(), 2, commandArgs.length())).trim();
            if (commandToExecute.startsWith("/")) {
                commandToExecute = commandToExecute.substring(1);
            }
            target.performCommand(commandToExecute);
            send(commandArgs.getSender(), "sudo.command-success",
                    "&aForced {target} to execute: /{command}",
                    "{target}", target.getName(),
                    "{command}", commandToExecute);
            return;
        }

        send(commandArgs.getSender(), "sudo.invalid-mode",
                "&cInvalid mode. Use command or chat.");
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 0 || args.length() == 1) {
            return completeOnlinePlayers(args);
        }

        if (args.length() == 2) {
            return completeCurrentArg(args, Arrays.asList("command", "chat"));
        }

        return Collections.emptyList();
    }
}





