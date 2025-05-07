package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class MuteCommand implements CommandExecutor {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public MuteCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.mute")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to mute players.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /mute <player> <reason> [duration]");
            return false;
        }

        String playerName = args[0];
        String durationString = "permanent"; // Default to permanent mute
        String reason;

        // If duration is provided, use it
        if (args[args.length - 1].matches("^[0-9]+[smhd]$")) {
            durationString = args[args.length - 1];
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
        } else {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        if (reason.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You must provide a reason for the mute.");
            return false;
        }

        String issuer = sender.getName();
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();

        // Create a final copy of durationString for use in the lambda
        final String finalDurationString = durationString;

        // Check if the player is already muted
        databaseManager.isPlayerMuted(targetUUID, isMuted -> {
            if (isMuted) {
                sender.sendMessage(ChatColor.RED + "This player is already muted.");
                return;
            }

            // Calculate expiration time if duration is provided
            Instant expirationTime = null;
            if (!finalDurationString.equalsIgnoreCase("permanent")) {
                Duration duration = parseDuration(finalDurationString);
                if (duration == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid duration format. Use numbers followed by s, m, h, or d.");
                    return;
                }
                expirationTime = Instant.now().plus(duration);
            }

            // Add the mute to the database
            databaseManager.mutePlayer(targetUUID, reason, issuer, expirationTime);

            // Notify the issuer and the muted player if online
            sender.sendMessage(ChatColor.GREEN + "You have muted " + playerName + " " +
                    (finalDurationString.equalsIgnoreCase("permanent") ? "permanently" : "for " + finalDurationString) +
                    " for: " + reason);

            Player target = Bukkit.getPlayerExact(playerName);
            if (target != null) {
                target.sendMessage(ChatColor.RED + "You have been muted " +
                        (finalDurationString.equalsIgnoreCase("permanent") ? "permanently" : "for " + finalDurationString) +
                        " for: " + reason);
            }
        });

        return true;
    }

    private Duration parseDuration(String input) {
        try {
            char unit = input.charAt(input.length() - 1);
            long value = Long.parseLong(input.substring(0, input.length() - 1));
            switch (unit) {
                case 's':
                    return Duration.ofSeconds(value);
                case 'm':
                    return Duration.ofMinutes(value);
                case 'h':
                    return Duration.ofHours(value);
                case 'd':
                    return Duration.ofDays(value);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}