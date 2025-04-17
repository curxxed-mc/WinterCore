package curxxed.dev.icore.Commands.Gamemode;

import curxxed.dev.icore.Commands.Staff.StaffModeCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class gma implements CommandExecutor {

    private final StaffModeCommand staffModeCommand;

    public gma(StaffModeCommand staffModeCommand) {
        this.staffModeCommand = staffModeCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player is in staff mode
        if (staffModeCommand.isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You cannot change your game mode while in staff mode!");
            return true;
        }

        if (!player.hasPermission("iCore.gamemode")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        Player target;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
        } else {
            target = player;
        }

        target.setGameMode(GameMode.ADVENTURE);
        target.sendMessage(ChatColor.AQUA + "Your game mode has been set to Adventure Mode.");
        if (!target.equals(player)) {
            player.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s game mode to Adventure.");
        }
        return true;
    }
}