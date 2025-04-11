package hyp.ilfov.i.icore.Commands.misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        Player target = player;

        if (args.length == 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("reset")) {
            target.setWalkSpeed(0.2f);
            target.setFlySpeed(0.1f);
            target.sendMessage(ChatColor.GREEN + "Your speed has been reset.");
            if (target != player) {
                player.sendMessage(ChatColor.GREEN + "You reset " + target.getName() + "'s speed.");
            }
            return true;
        }

        try {
            int speed = Integer.parseInt(args[0]);

            if (speed < 1 || speed > 10) {
                player.sendMessage(ChatColor.RED + "Speed must be between 1 and 10.");
                return true;
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

        return true;
    }
}
