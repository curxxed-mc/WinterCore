package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandInfo(
        name = "warning",
        permission = "WinterCore.warning",
        description = "Warn a player.",
        aliases = {"warn"},
        usage = "/warn <player> <reason>",
        inGameOnly = false
    
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
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /warn <player> <reason>");
            return;
        }
        String playerName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String issuer = commandArgs.getSender().getName();
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        moderationService.addWarning(displayName, reason, issuer);
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have warned " + displayName + " for: " + reason);
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been warned for: " + reason);
        }
    }
}
