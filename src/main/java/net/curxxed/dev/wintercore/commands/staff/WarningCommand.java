package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "warning",
        description = "Warn a player.",
        aliases = {"warn"},
        usage = "/warn <player> <reason>",
        inGameOnly = false,
        permission = {"wintercore.warning", "WinterCore.warning"}
)
public class WarningCommand extends BaseCommand {
    private final WinterCore plugin;
    private final ModerationService moderationService;

    public WarningCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.moderationService = plugin.getDatabaseManager().getModerationService();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        String[] args = commandArgs.getArgs();
        if (args.length < 2) {
            sendUsage(commandArgs.getSender());
            return;
        }
        String playerName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String issuer = commandArgs.getSender().getName();
        resolvePlayerId(commandArgs.getSender(), playerName, targetUUID -> {
            moderationService.addWarning(playerName, reason, issuer);
            send(commandArgs.getSender(), "moderation.warning.actor-success",
                    "&aYou have warned {target} for: {reason}",
                    "{target}", playerName,
                    "{reason}", reason);
            Player target = plugin.getServer().getPlayer(targetUUID);
            if (target != null) {
                send(target, "moderation.warning.target-success",
                        "&cYou have been warned for: {reason}",
                        "{reason}", reason);
            }
        });
    }
}




