package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "fly",
            permission = "WinterCore.fly",
            description = "Toggle flight for yourself or another player.",
            usage = "/fly [player]",
            inGameOnly = true
    
    )
public class Fly extends BaseCommand {

    public Fly(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player senderPlayer = commandArgs.getPlayer();
        boolean isPlayer = commandArgs.isPlayer();

        if (commandArgs.length() == 0) {

            toggleFlight(senderPlayer, senderPlayer);
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found!");
            return;
        }

        toggleFlight(commandArgs.getSender(), target);
    }

    private void toggleFlight(org.bukkit.command.CommandSender sender, Player target) {
        target.setAllowFlight(!target.getAllowFlight());

        if (target.getAllowFlight()) {
            target.sendMessage(ChatColor.GREEN + "You can now fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.GREEN + target.getName() + " can now fly!");
            }
        } else {
            target.sendMessage(ChatColor.RED + "You can no longer fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(ChatColor.RED + target.getName() + " can no longer fly!");
            }
        }
    }
}