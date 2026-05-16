package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

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
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = playerName;
        }
        moderationService.addWarning(displayName, reason, issuer);
        send(commandArgs.getSender(), "moderation.warning.actor-success",
                "&aYou have warned {target} for: {reason}",
                "{target}", displayName,
                "{reason}", reason);
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            send(target, "moderation.warning.target-success",
                    "&cYou have been warned for: {reason}",
                    "{reason}", reason);
        }
    }
}





