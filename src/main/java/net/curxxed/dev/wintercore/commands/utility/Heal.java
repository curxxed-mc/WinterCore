package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "heal",
        description = "Heal yourself or another player.",
        usage = "/heal [player]",
        inGameOnly = false,
        permission = {"wintercore.heal", "WinterCore.heal"}
)
public class Heal extends BaseCommand {

    public Heal(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player senderPlayer = commandArgs.getPlayer();
        Player target;

        if (commandArgs.length() > 0) {
            target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
            if (target == null || !target.isOnline()) {
                commandArgs.getSender().sendMessage(CC.RED + "Player not found!");
                return;
            }
        } else if (commandArgs.isPlayer()) {
            target = senderPlayer;
        } else {
            commandArgs.getSender().sendMessage(CC.RED + "Only players can use this command on themselves!");
            return;
        }

        target.setHealth(20);
        target.sendMessage(CC.GREEN + "Your health has been restored!");
        if (!target.equals(commandArgs.getSender())) {
            commandArgs.getSender().sendMessage(CC.GREEN + "You have healed " + target.getName() + "!");
        }
    }
}




