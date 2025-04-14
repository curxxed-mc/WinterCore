package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnmuteCommand implements CommandExecutor {
    private final Main plugin;

    public UnmuteCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unmute <player>");
            return false;
        }

        String playerName = args[0];
        PunishmentManager punishmentManager = plugin.getPunishmentManager();

        // Check if the player is muted
        if (!punishmentManager.isPlayerMuted(playerName)) {
            sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not muted.");
            return false;
        }

        // Remove the mute
        punishmentManager.removePunishment(playerName, "mutes");

        // Notify the sender and the player if online
        sender.sendMessage(ChatColor.GREEN + "You have unmuted " + playerName + ".");
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage(ChatColor.GREEN + "You have been unmuted.");
        }

        return true;
    }
}
