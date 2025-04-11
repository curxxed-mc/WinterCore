package hyp.ilfov.i.icore.Commands.Staff;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.PunishmentManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteCommand implements CommandExecutor {
    private final Main plugin;

    public MuteCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mute <player> <reason> [duration]");
            return false;
        }

        String playerName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
        String duration = "permanent"; // Default to permanent mute

        // If duration is provided, use it
        if (!args[args.length - 1].matches("^[a-zA-Z0-9]+[smhd]$")) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)); // No duration, just reason
        } else {
            duration = args[args.length - 1]; // Set duration
        }

        String issuer = sender.getName();

        // Add the mute to punishment data
        PunishmentManager punishmentManager = plugin.getPunishmentManager();
        punishmentManager.addPunishment(playerName, "mute", reason, issuer, duration);

        // Notify the issuer and the muted player if online
        sender.sendMessage(ChatColor.GREEN + "You have muted " + playerName + " " +
                (duration.equalsIgnoreCase("permanent") ? "permanently" : "for " + duration) +
                " for: " + reason);

        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been muted " +
                    (duration.equalsIgnoreCase("permanent") ? "permanently" : "for " + duration) +
                    " for: " + reason);
        }

        return true;
    }
}
