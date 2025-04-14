package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.PunishmentManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import java.time.Duration;
import java.time.Instant;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class TempBanCommand implements CommandExecutor {
    private final Main plugin;

    public TempBanCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        // Check if player has permission to temp ban
        if (!player.hasPermission("iCore.tempban")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to temporarily ban players.");
            return false;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /tban <player> <duration> <reason>");
            return false;
        }

        String targetName = args[0];
        String durationString = args[1];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return false;
        }

        Duration duration = parseDuration(durationString);
        if (duration == null) {
            player.sendMessage(ChatColor.RED + "Invalid duration format. Use a format like 1h, 30m, or 2d.");
            return false;
        }

        Instant expirationTime = Instant.now().plus(duration);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedExpiration = sdf.format(Date.from(expirationTime));

        PunishmentManager punishmentManager = new PunishmentManager(plugin);
        punishmentManager.addPunishment(targetName, "tempban", reason, player.getName(), formattedExpiration);

        target.kickPlayer(ChatColor.RED + "You have been temporarily banned by " + player.getName() + ".\n" +
                "Reason: " + reason + "\n" +
                "Duration: " + durationString + "\n" +
                "Expires: " + formattedExpiration);

        plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(
                targetName,
                "Temporarily banned by " + player.getName() + "\nReason: " + reason,
                Date.from(expirationTime),
                null
        );

        player.sendMessage(ChatColor.GREEN + "Player " + targetName + " has been temporarily banned for " + durationString + ". Reason: " + reason);
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
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean checkBan(String playerName, Main plugin, PlayerLoginEvent event) {
        PunishmentManager punishmentManager = new PunishmentManager(plugin);

        if (punishmentManager.isPlayerBanned(playerName)) {
            if (punishmentManager.isBanExpired(playerName)) {
                // Remove expired ban
                punishmentManager.removeBan(playerName);
                return false; // Player is not banned anymore
            }

            // Fetch ban details
            Map<String, Object> banDetails = punishmentManager.getBanDetails(playerName);
            if (banDetails != null) {
                String reason = (String) banDetails.get("reason");
                String expirationTime = (String) banDetails.get("expirationTime");

                // Deny login with a message
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        ChatColor.RED + "You are temporarily banned!\n" +
                                ChatColor.YELLOW + "Reason: " + reason + "\n" +
                                ChatColor.YELLOW + "Expires: " + expirationTime);
                return true; // Player is banned
            }
        }
        return false; // Player is not banned
    }
}
