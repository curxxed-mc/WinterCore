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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class BanCommand implements CommandExecutor {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public BanCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iCore.ban")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to ban players.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ban <player> [duration] <reason> [-s]");
            return false;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return false;
        }

        String playerName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String durationString = args[1];
        String reason;

        // Check if the second argument is a duration or part of the reason
        Duration duration = parseDuration(durationString);
        if (duration != null) {
            // If valid duration, extract the reason after it
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "You must provide a reason for the ban.");
                return false;
            }
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, silent ? args.length - 1 : args.length));
        } else {
            // If no valid duration, treat the second argument as part of the reason
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, silent ? args.length - 1 : args.length));
        }

        if (reason.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "You must provide a reason for the ban.");
            return false;
        }

        // Check if the player is already banned
        databaseManager.isPlayerBanned(targetUUID, isBanned -> {
            if (isBanned) {
                sender.sendMessage(ChatColor.RED + "This player is already banned.");
                return;
            }

            if (duration != null) {
                // Temporary ban
                Instant expirationTime = Instant.now().plus(duration);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedExpiration = sdf.format(Date.from(expirationTime));

                databaseManager.banPlayer(targetUUID, reason, expirationTime); // Pass expirationTime
                target.kickPlayer(ChatColor.RED + "You have been temporarily banned by " + sender.getName() + ".\n" +
                        "Reason: " + reason + "\n" +
                        "Expires: " + formattedExpiration);

                broadcastBanMessage(targetName, reason, "until " + formattedExpiration, sender.getName(), silent);
            } else {
                // Permanent ban
                databaseManager.banPlayer(targetUUID, reason, null); // Pass null for permanent bans
                target.kickPlayer(ChatColor.RED + "You have been permanently banned by " + sender.getName() + ".\nReason: " + reason);

                broadcastBanMessage(targetName, reason, "permanently", sender.getName(), silent);
            }
        });

        return true;
    }

    private Duration parseDuration(String durationString) {
        try {
            if (durationString.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(durationString.substring(0, durationString.length() - 1)));
            } else if (durationString.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(durationString.substring(0, durationString.length() - 1)));
            } else if (durationString.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(durationString.substring(0, durationString.length() - 1)));
            } else if (durationString.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(durationString.substring(0, durationString.length() - 1)));
            } else {
                return null;

            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void broadcastBanMessage(String targetName, String reason, String duration, String issuer, boolean silent) {
        String message = ChatColor.RED + targetName + " has been banned " + duration + " by " + issuer + ". Reason: " + reason;
        if (silent) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("iCore.staff"))
                    .forEach(player -> player.sendMessage(message));
        } else {
            Bukkit.broadcastMessage(message);
        }
    }
}