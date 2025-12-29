package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandInfo(
        name = "unmute",
        permission = "WinterCore.unmute",
        description = "Unmute a player.",
        usage = "/unmute <player>",
        inGameOnly = false
    
    )
public class UnmuteCommand extends BaseCommand {
    private final WinterCore plugin;
    private final DatabaseManager databaseManager;

    public UnmuteCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (!commandArgs.getSender().hasPermission("WinterCore.unmute")) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou do not have permission to unmute players."));
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length < 1) {
            commandArgs.getSender().sendMessage(CC.translate("&cUsage: /unmute <player>"));
            return;
        }
        String playerName = args[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String displayName = Bukkit.getOfflinePlayer(targetUUID).getName();
        databaseManager.isPlayerMuted(targetUUID, isMuted -> {
            if (!isMuted) {
                commandArgs.getSender().sendMessage(CC.translate("&cPlayer " + displayName + " is not muted."));
                return;
            }
            databaseManager.unmutePlayer(targetUUID);
            commandArgs.getSender().sendMessage(CC.translate("&aYou have unmuted " + displayName + "."));
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                target.sendMessage(CC.translate("&aYou have been unmuted."));
            }
        });
    }
}
