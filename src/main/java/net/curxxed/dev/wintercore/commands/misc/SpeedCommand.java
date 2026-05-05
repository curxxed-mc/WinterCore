package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandInfo(
        name = "speed",
        description = "Change your or another player's speed.",
        usage = "/speed <1-10|reset> [player]",
        inGameOnly = true,
        permission = {"wintercore.speed", "WinterCore.speed"}
)
public class SpeedCommand extends BaseCommand {

    public SpeedCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();
        if (player == null && args.length < 2) {
            commandArgs.getSender().sendMessage(CC.RED + "Only players can use this command.");
            return;
        }
        Player target = player;
        if (args.length == 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(CC.RED + "Player not found.");
                return;
            }
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("reset")) {
            target.setWalkSpeed(0.2f);
            target.setFlySpeed(0.1f);
            target.sendMessage(CC.GREEN + "Your speed has been reset.");
            if (target != player) {
                player.sendMessage(CC.GREEN + "You reset " + target.getName() + "'s speed.");
            }
            return;
        }
        try {
            int speed = Integer.parseInt(args[0]);
            if (speed < 1 || speed > 10) {
                player.sendMessage(CC.RED + "Speed must be between 1 and 10.");
                return;
            }
            float speedValue = speed / 10.0f;
            if (target.isFlying()) {
                target.setFlySpeed(speedValue);
            } else {
                target.setWalkSpeed(speedValue);
            }
            target.sendMessage(CC.GREEN + "Your speed has been set to " + speed + ".");
            if (target != player) {
                player.sendMessage(CC.GREEN + "You set " + target.getName() + "'s speed to " + speed + ".");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(CC.RED + "Invalid speed value.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            return completeCurrentArg(args, Arrays.asList("reset", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));
        }

        if (args.length() == 2) {
            return completeOnlinePlayers(args);
        }

        return Collections.emptyList();
    }
}





