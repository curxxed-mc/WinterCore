package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class MuteCommand extends BaseCommand {
    private final WinterCore plugin;
    private final DatabaseManager databaseManager;

    public MuteCommand(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "mute",
        permission = "WinterCore.mute",
        description = "Mute a player.",
        usage = "/mute <player> <reason> [duration]",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("WinterCore.mute")) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou do not have permission to mute players."));
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 2) {
            commandArgs.getSender().sendMessage(CC.translate("&cUsage: /mute <player> <reason> [duration]"));
            return;
        }
        String playerName = args[0];
        String durationString = "permanent";
        String reason;
        if (args[args.length - 1].matches("^[0-9]+[smhd]$")) {
            durationString = args[args.length - 1];
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
        } else {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        Duration duration = parseDuration(durationString);
        databaseManager.isPlayerMuted(targetUUID, isMuted -> {
            if (isMuted) {
                commandArgs.getSender().sendMessage(CC.translate("&cPlayer " + displayName + " is already muted."));
                return;
            }
            Instant expiration = duration != null ? Instant.now().plus(duration) : null;
            databaseManager.mutePlayer(targetUUID, reason, commandArgs.getSender().getName(), expiration);
            commandArgs.getSender().sendMessage(CC.translate("&aYou have muted " + displayName + (expiration != null ? " until " + expiration : " permanently") + "."));
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                target.sendMessage(CC.translate("&cYou have been muted for: " + reason + (expiration != null ? " until " + expiration : " permanently")));
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
}
