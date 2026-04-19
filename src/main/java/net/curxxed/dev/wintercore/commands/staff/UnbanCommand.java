package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandInfo(
        name = "unban",
        permission = "WinterCore.unban",
        description = "Unban a player.",
        usage = "/unban <player>",
        inGameOnly = false
    
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
        if (!commandArgs.getSender().hasPermission("WinterCore.unban")) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou do not have permission to unban players."));
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            commandArgs.getSender().sendMessage(CC.translate("&cUsage: /unban <player>"));
            return;
        }
        String targetName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        // Always get the correct capitalization for the IGN
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        moderationService.isPlayerBanned(targetUUID, isBanned -> {
            if (!isBanned) {
                commandArgs.getSender().sendMessage(CC.translate("&cPlayer " + displayName + " is not banned."));
                return;
            }
            moderationService.unbanPlayer(targetUUID);
            commandArgs.getSender().sendMessage(CC.translate("&aPlayer " + displayName + " has been unbanned."));
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                target.sendMessage(CC.translate("&aYou have been unbanned."));
            }
        });
    }
}
