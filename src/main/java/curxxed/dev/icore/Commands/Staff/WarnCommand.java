package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarnCommand implements CommandExecutor {
    private final Main plugin;

    public WarnCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /warn <player> <reason>");
            return false;
        }

        String playerName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String issuer = sender.getName();

        // Add the warning to punishment data
        plugin.getPunishmentManager().addPunishment(playerName, "warnings", reason, issuer, null);

        // Notify the issuer and the warned player if online
        sender.sendMessage(ChatColor.GREEN + "You have warned " + playerName + " for: " + reason);
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been warned for: " + reason);
        }

        return true;
    }
}
