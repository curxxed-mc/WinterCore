package net.curxxed.dev.icore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SpeedCommand extends BaseCommand {

    @Command(
        name = "speed",
        permission = "iCore.speed",
        description = "Change your or another player's speed.",
        usage = "/speed <1-10|reset> [player]",
        inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();
        if (player == null && args.length < 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        Player target = player;
        if (args.length == 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return;
            }
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("reset")) {
            target.setWalkSpeed(0.2f);
            target.setFlySpeed(0.1f);
            target.sendMessage(ChatColor.GREEN + "Your speed has been reset.");
            if (target != player) {
                player.sendMessage(ChatColor.GREEN + "You reset " + target.getName() + "'s speed.");
            }
            return;
        }
        try {
            int speed = Integer.parseInt(args[0]);
            if (speed < 1 || speed > 10) {
                player.sendMessage(ChatColor.RED + "Speed must be between 1 and 10.");
                return;
            }
            float speedValue = speed / 10.0f;
            if (target.isFlying()) {
                target.setFlySpeed(speedValue);
            } else {
                target.setWalkSpeed(speedValue);
            }
            target.sendMessage(ChatColor.GREEN + "Your speed has been set to " + speed + ".");
            if (target != player) {
                player.sendMessage(ChatColor.GREEN + "You set " + target.getName() + "'s speed to " + speed + ".");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid speed value.");
        }
    }
}
