package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

@CommandInfo(
        name = "kick",
        description = "Kick players from the server.",
        usage = "/kick <player> [reason]",
        inGameOnly = true,
        permission = {"wintercore.kick", "WinterCore.kick"}
)
public class KickCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public KickCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();
        if (player == null) {
            send(commandArgs.getSender(), "moderation.kick.player-only",
                    "&cOnly players can use this command.");
            return;
        }
        if (args.length < 1) {
            sendUsage(player);
            return;
        }
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            send(player, "general.player-not-found", "&cPlayer not found.");
            return;
        }
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : msg("moderation.kick.default-reason", "No reason provided");
        UUID targetUUID = target.getUniqueId();
        moderationService.incrementKickCount(targetUUID);
        target.kickPlayer(msg("moderation.kick.target-kick-message",
                "&cYou have been kicked for: {reason}",
                "{reason}", reason));
        send(player, "moderation.kick.actor-success",
                "&aPlayer {target} has been kicked for: {reason}",
                "{target}", target.getName(),
                "{reason}", reason);
    }
}




