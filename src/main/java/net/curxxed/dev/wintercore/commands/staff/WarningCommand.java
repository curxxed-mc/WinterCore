package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WarningCommand extends BaseCommand {
    private final WinterCore plugin;
    private final DatabaseManager databaseManager;

    public WarningCommand(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "warning",
        permission = "WinterCore.warning",
        description = "Warn a player.",
        aliases = {"warn"},
        usage = "/warn <player> <reason>",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
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
        databaseManager.addWarning(displayName, reason, issuer);
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have warned " + displayName + " for: " + reason);
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been warned for: " + reason);
        }
    }
}
