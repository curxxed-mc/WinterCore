package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.DurationParser;
import net.curxxed.dev.wintercore.config.ModerationMessages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandInfo(
        name = "mute",
        description = "Mute a player.",
        usage = "/mute <player> <reason> [duration]",
        inGameOnly = false,
        async = true,
        permission = {"wintercore.mute", "WinterCore.mute"}
)
public class MuteCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public MuteCommand(WinterCore plugin) {
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

        if (args.length() >= 3) {
            String current = args.getOptionalString(args.length() - 1).orElse("");
            if (current.isEmpty() || current.matches("(?i)^\\d*[smhdw]?$")) {
                return completeCurrentArg(args, Arrays.asList("10m", "30m", "1h", "6h", "1d", "7d", "1w", "30d"));
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

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (reason.isEmpty()) {
            send(commandArgs.getSender(), "moderation.mute.reason-required",
                    "&cYou must provide a reason for the mute.");
            return;
        }

        Duration duration = null;
        if (args.length >= 3) {
            Duration parsed = DurationParser.parse(args[args.length - 1]);
            if (parsed != null) {
                duration = parsed;
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1)).trim();
                if (reason.isEmpty()) {
                    send(commandArgs.getSender(), "moderation.mute.reason-required",
                            "&cYou must provide a reason for the mute.");
                    return;
                }
            }
        }

        final String finalReason = reason;
        final Duration finalDuration = duration;
        resolvePlayerId(commandArgs.getSender(), playerName, targetUUID ->
                moderationService.isPlayerMuted(targetUUID, isMuted -> runSync(() -> {
            if (isMuted) {
                send(commandArgs.getSender(), "moderation.mute.already-muted",
                        "&cPlayer {target} is already muted.",
                        "{target}", playerName);
                return;
            }

            Instant expiration = finalDuration != null ? Instant.now().plus(finalDuration) : null;
            moderationService.mutePlayer(targetUUID, finalReason, commandArgs.getSender().getName(), expiration);

            if (expiration != null) {
                send(commandArgs.getSender(), "moderation.mute.actor-success-temporary",
                        "&aYou have muted {target} until {until}.",
                        "{target}", playerName,
                        "{until}", ModerationMessages.formatTimestamp(expiration.toEpochMilli()));
            } else {
                send(commandArgs.getSender(), "moderation.mute.actor-success-permanent",
                        "&aYou have muted {target} permanently.",
                        "{target}", playerName);
            }

            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                if (expiration != null) {
                    send(target, "moderation.mute.target-notice-temporary",
                            "&cYou have been muted for: {reason} until {until}.",
                            "{reason}", finalReason,
                            "{until}", ModerationMessages.formatTimestamp(expiration.toEpochMilli()));
                } else {
                    send(target, "moderation.mute.target-notice-permanent",
                            "&cYou have been muted for: {reason} permanently.",
                            "{reason}", finalReason);
                }
            }
                })));
    }
}
