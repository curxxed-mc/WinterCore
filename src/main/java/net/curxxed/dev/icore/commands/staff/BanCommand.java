package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.time.Instant;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;


public class BanCommand extends BaseCommand {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public BanCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "ban",
        permission = "iCore.ban",
        description = "Ban a player from the server.",
        usage = "/ban <player> [duration] <reason> [-s]",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("iCore.ban")) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "You do not have permission to ban players.");
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /ban <player> [duration] <reason> [-s]");
            return;
        }
        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return;
        }
        String playerName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        boolean silent = args[args.length - 1].equalsIgnoreCase("-s");
        String durationString = args[1];
        String reason;
        Duration duration = parseDuration(durationString);
        if (duration != null) {
            if (args.length < 3) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "You must provide a reason for the ban.");
                return;
            }
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, silent ? args.length - 1 : args.length));
        } else {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, silent ? args.length - 1 : args.length));
        }
        if (reason.isEmpty()) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "You must provide a reason for the ban.");
            return;
        }
        databaseManager.isPlayerBanned(targetUUID, isBanned -> {
            if (isBanned) {
                commandArgs.getSender().sendMessage(ChatColor.RED + "This player is already banned.");
                return;
            }
            if (duration != null) {
                Instant expirationTime = Instant.now().plus(duration);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedExpiration = sdf.format(Date.from(expirationTime));
                databaseManager.banPlayer(targetUUID, reason, expirationTime);
                target.kickPlayer(ChatColor.RED + "You have been temporarily banned by " + commandArgs.getSender().getName() + ".\n" +
                        "Reason: " + reason + "\n" +
                        "Expires: " + formattedExpiration);
                broadcastBanMessage(targetName, reason, "until " + formattedExpiration, commandArgs.getSender().getName(), silent);
            } else {
                databaseManager.banPlayer(targetUUID, reason, null);
                target.kickPlayer(ChatColor.RED + "You have been permanently banned by " + commandArgs.getSender().getName() + ".\nReason: " + reason);
                broadcastBanMessage(targetName, reason, "permanently", commandArgs.getSender().getName(), silent);
            }
        });
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

