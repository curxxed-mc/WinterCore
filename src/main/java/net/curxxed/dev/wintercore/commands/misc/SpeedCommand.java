package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
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
        Player target = player;
        if (args.length == 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                send(player, "speed.player-not-found", "&cPlayer not found.");
                return;
            }
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("reset")) {
            target.setWalkSpeed(0.2f);
            target.setFlySpeed(0.1f);
            send(target, "speed.reset-success", "&aYour speed has been reset.");
            if (target != player) {
                send(player, "speed.reset-other", "&aYou reset {target}'s speed.", "{target}", target.getName());
            }
            return;
        }
        try {
            int speed = Integer.parseInt(args[0]);
            if (speed < 1 || speed > 10) {
                send(player, "speed.invalid-range", "&cSpeed must be between 1 and 10.");
                return;
            }
            float speedValue = speed / 10.0f;
            if (target.isFlying()) {
                target.setFlySpeed(speedValue);
            } else {
                target.setWalkSpeed(speedValue);
            }
            send(target, "speed.set-success", "&aYour speed has been set to {speed}.", "{speed}", String.valueOf(speed));
            if (target != player) {
                send(player, "speed.set-other", "&aYou set {target}'s speed to {speed}.", "{target}", target.getName(), "{speed}", String.valueOf(speed));
            }
        } catch (NumberFormatException e) {
            send(player, "speed.invalid-value", "&cInvalid speed value.");
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
