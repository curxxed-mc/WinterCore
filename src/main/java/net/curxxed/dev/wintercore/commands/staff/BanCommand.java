package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ModerationActionPacket;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.DurationParser;
import net.curxxed.dev.wintercore.config.ModerationMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@CommandInfo(
        name = "ban",
        description = "Ban a player from the server.",
        usage = "/ban <player> [duration] <reason> [-s]",
        inGameOnly = false,
        async = true,
        permission = {"wintercore.ban", "WinterCore.ban"}
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
        runSync(() -> executeOnMainThread(commandArgs));
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 0 || args.length() == 1) {
            return completeOnlinePlayers(args);
        }

        if (args.length() == 2) {
            return completeCurrentArg(args, Arrays.asList("10m", "30m", "1h", "6h", "1d", "7d", "1w", "-s", "--silent"));
        }

        if (args.length() >= 3) {
            String current = args.getOptionalString(args.length() - 1).orElse("");
            if (current.startsWith("-")) {
                return completeCurrentArg(args, Arrays.asList("-s", "--silent"));
            }
        }

        return Collections.emptyList();
    }

    private void executeOnMainThread(CommandArguments commandArgs) {

        String[] args = commandArgs.getArgs();
        if (args.length < 2) {
            sendUsage(commandArgs.getSender());
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
            send(commandArgs.getSender(), "moderation.ban.reason-required",
                    "&cYou must provide a reason for the ban.");
            return;
        }

        java.time.Duration duration = DurationParser.parse(remainingArgs.get(0));
        String reason;
        if (duration != null) {
            if (remainingArgs.size() < 2) {
                send(commandArgs.getSender(), "moderation.ban.reason-required",
                        "&cYou must provide a reason for the ban.");
                return;
            }
            reason = String.join(" ", remainingArgs.subList(1, remainingArgs.size())).trim();
        } else {
            reason = String.join(" ", remainingArgs).trim();
        }

        if (reason.isEmpty()) {
            send(commandArgs.getSender(), "moderation.ban.reason-required",
                    "&cYou must provide a reason for the ban.");
            return;
        }

        String issuer = commandArgs.getSender().getName();
        String serverName = plugin.getConfig().getString("server-name", "Unknown");

        moderationService.isPlayerBanned(targetUUID, isBanned -> runSync(() -> {
            if (isBanned) {
                send(commandArgs.getSender(), "moderation.ban.already-banned",
                        "&cThis player is already banned.");
                return;
            }

            if (duration != null) {
                Instant expirationTime = Instant.now().plus(duration);
                moderationService.banPlayer(targetUUID, reason, expirationTime);

                if (target != null && target.isOnline()) {
                    target.kickPlayer(ModerationMessages.formatBanKickMessage(issuer, reason, expirationTime.toEpochMilli()));
                }

                send(commandArgs.getSender(), "moderation.ban.actor-success-temporary",
                        "&aBanned &f{target}&a until &f{until}&a.",
                        "{target}", displayName,
                        "{until}", ModerationMessages.formatTimestamp(expirationTime.toEpochMilli()));

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

                send(commandArgs.getSender(), "moderation.ban.actor-success-permanent",
                        "&aBanned &f{target}&a permanently.",
                        "{target}", displayName);

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
        }));
    }
}
