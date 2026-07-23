package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ModerationActionPacket;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandInfo(
        name = "unban",
        description = "Unban a player.",
        usage = "/unban <player>",
        inGameOnly = false,
        async = true,
        permission = {"wintercore.unban", "WinterCore.unban"}
)
public class UnbanCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public UnbanCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        runSync(() -> executeOnMainThread(commandArgs));
    }

    private void executeOnMainThread(CommandArguments commandArgs) {
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            sendUsage(commandArgs.getSender());
            return;
        }
        String targetName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = targetName;
        }
        final String finalDisplayName = displayName;
        moderationService.isPlayerBanned(targetUUID, isBanned -> runSync(() -> {
            if (!isBanned) {
                send(commandArgs.getSender(), "moderation.unban.not-banned",
                        "&cPlayer {target} is not banned.",
                        "{target}", finalDisplayName);
                return;
            }

            moderationService.unbanPlayer(targetUUID);
            send(commandArgs.getSender(), "moderation.unban.actor-success",
                    "&aPlayer {target} has been unbanned.",
                    "{target}", finalDisplayName);

            plugin.getRedisManager().publishAndHandleLocally(new ModerationActionPacket(
                    plugin.getConfig().getString("server-name", "Unknown"),
                    System.currentTimeMillis(),
                    ModerationActionPacket.ActionType.BAN_REMOVED,
                    targetUUID,
                    finalDisplayName,
                    commandArgs.getSender().getName(),
                    "",
                    null,
                    false
            ));

            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                send(target, "moderation.unban.target-success",
                        "&aYou have been unbanned.");
            }
        }));
    }
}
