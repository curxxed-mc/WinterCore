package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ModerationActionPacket;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ModerationMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@CommandInfo(
        name = "ban",
        permission = "WinterCore.ban",
        description = "Ban a player from the server.",
        usage = "/ban <player> [duration] <reason> [-s]",
        inGameOnly = false
    
    )
public class BanCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public BanCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        if (!commandArgs.getSender().hasPermission("WinterCore.ban")) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou do not have permission to ban players."));
            return;
        }

        String[] args = commandArgs.getArgs();
        if (args.length < 2) {
            commandArgs.getSender().sendMessage(CC.translate("&cUsage: /ban <player> [duration] <reason> [-s]"));
            return;
        }

        String targetName = args[0];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = offlineTarget.getUniqueId();
        Player target = plugin.getServer().getPlayer(targetUUID);
        String displayName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;

        List<String> remainingArgs = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
        boolean silent = remainingArgs.removeIf(arg -> arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("--silent"));

        if (remainingArgs.isEmpty()) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou must provide a reason for the ban."));
            return;
        }

        Duration duration = parseDuration(remainingArgs.get(0));
        String reason;
        if (duration != null) {
            if (remainingArgs.size() < 2) {
                commandArgs.getSender().sendMessage(CC.translate("&cYou must provide a reason for the ban."));
                return;
            }
            reason = String.join(" ", remainingArgs.subList(1, remainingArgs.size())).trim();
        } else {
            reason = String.join(" ", remainingArgs).trim();
        }

        if (reason.isEmpty()) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou must provide a reason for the ban."));
            return;
        }

        String issuer = commandArgs.getSender().getName();
        String serverName = plugin.getConfig().getString("server-name", "Unknown");

        moderationService.isPlayerBanned(targetUUID, isBanned -> {
            if (isBanned) {
                commandArgs.getSender().sendMessage(CC.translate("&cThis player is already banned."));
                return;
            }

            if (duration != null) {
                Instant expirationTime = Instant.now().plus(duration);
                moderationService.banPlayer(targetUUID, reason, expirationTime);

                if (target != null && target.isOnline()) {
                    target.kickPlayer(ModerationMessages.formatBanKickMessage(issuer, reason, expirationTime.toEpochMilli()));
                }

                commandArgs.getSender().sendMessage(CC.translate("&aBanned &f" + displayName + "&a until &f"
                        + ModerationMessages.formatTimestamp(expirationTime.toEpochMilli()) + "&a."));

                plugin.getRedisManager().publishAndHandleLocally(new ModerationActionPacket(
                        serverName,
                        System.currentTimeMillis(),
                        ModerationActionPacket.ActionType.BAN_APPLIED,
                        targetUUID,
                        displayName,
                        issuer,
                        reason,
                        expirationTime.toEpochMilli(),
                        silent
                ));
            } else {
                moderationService.banPlayer(targetUUID, reason, null);

                if (target != null && target.isOnline()) {
                    target.kickPlayer(ModerationMessages.formatBanKickMessage(issuer, reason, null));
                }

                commandArgs.getSender().sendMessage(CC.translate("&aBanned &f" + displayName + "&a permanently."));

                plugin.getRedisManager().publishAndHandleLocally(new ModerationActionPacket(
                        serverName,
                        System.currentTimeMillis(),
                        ModerationActionPacket.ActionType.BAN_APPLIED,
                        targetUUID,
                        displayName,
                        issuer,
                        reason,
                        null,
                        silent
                ));
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
