package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "fly",
        description = "Toggle flight for yourself or another player.",
        usage = "/fly [player]",
        inGameOnly = true,
        permission = {"wintercore.fly", "WinterCore.fly"}
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
            commandArgs.getSender().sendMessage(CC.RED + "Player not found!");
            return;
        }

        toggleFlight(commandArgs.getSender(), target);
    }

    private void toggleFlight(org.bukkit.command.CommandSender sender, Player target) {
        target.setAllowFlight(!target.getAllowFlight());

        if (target.getAllowFlight()) {
            target.sendMessage(CC.GREEN + "You can now fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(CC.GREEN + target.getName() + " can now fly!");
            }
        } else {
            target.sendMessage(CC.RED + "You can no longer fly!");
            if (!target.equals(sender)) {
                sender.sendMessage(CC.RED + target.getName() + " can no longer fly!");
            }
        }
    }
}




