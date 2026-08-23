package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "unmute",
        description = "Unmute a player.",
        usage = "/unmute <player>",
        inGameOnly = false,
        async = true,
        permission = {"wintercore.unmute", "WinterCore.unmute"}
)
public class UnmuteCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public UnmuteCommand(WinterCore plugin) {
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
        String playerName = args[0];
        resolvePlayerId(commandArgs.getSender(), playerName, targetUUID ->
                moderationService.isPlayerMuted(targetUUID, isMuted -> runSync(() -> {
                    if (!isMuted) {
                        send(commandArgs.getSender(), "moderation.unmute.not-muted",
                                "&cPlayer {target} is not muted.",
                                "{target}", playerName);
                        return;
                    }
                    moderationService.unmutePlayer(targetUUID);
                    send(commandArgs.getSender(), "moderation.unmute.actor-success",
                            "&aYou have unmuted {target}.",
                            "{target}", playerName);
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null) {
                        send(target, "moderation.unmute.target-success",
                                "&aYou have been unmuted.");
                    }
                })));
    }
}
