package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "feed",
        description = "Feed yourself or another player.",
        usage = "/feed [player]",
        inGameOnly = false,
        permission = {"wintercore.feed", "WinterCore.feed"}
)
public class Feed extends BaseCommand {

    public Feed(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() == 0) {
            if (!commandArgs.isPlayer()) {
                commandArgs.getSender().sendMessage(CC.RED + "Only players can use this command on themselves!");
                return;
            }
            Player player = commandArgs.getPlayer();
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.sendMessage(CC.GREEN + "You are now full.");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            commandArgs.getSender().sendMessage(CC.RED + "Player not found!");
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.sendMessage(CC.GREEN + "You have been fed by " + commandArgs.getSender().getName() + ".");
        commandArgs.getSender().sendMessage(CC.GREEN + "You have fed " + target.getName() + ".");
    }
}




