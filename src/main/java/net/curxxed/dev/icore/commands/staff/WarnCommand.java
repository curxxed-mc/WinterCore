package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class WarnCommand extends BaseCommand {
    private final iCore plugin;
    private final DatabaseManager databaseManager;

    public WarnCommand(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @Command(
        name = "warn",
        permission = "iCore.warn",
        description = "Warn a player.",
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
        databaseManager.addWarning(playerName, reason, issuer);
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "You have warned " + playerName + " for: " + reason);
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage(ChatColor.RED + "You have been warned for: " + reason);
        }
    }
}

