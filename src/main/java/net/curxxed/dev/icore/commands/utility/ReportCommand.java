package net.curxxed.dev.icore.commands.utility;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.tags.TagsManager;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ReportCommand extends BaseCommand {
    private final iCore plugin;
    private final TagsManager tagsManager;

    public ReportCommand(iCore plugin, TagsManager tagsManager) {
        this.plugin = plugin;
        this.tagsManager = tagsManager;
    }

    @Command(
            name = "report",
            description = "Report a player to the staff.",
            usage = "/report <player> <reason>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player reporter = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length < 2) {
            reporter.sendMessage(ChatColor.RED + "Usage: /report <player> <reason>");
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            reporter.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }

        if (target.equals(reporter)) {
            reporter.sendMessage(ChatColor.RED + "You cannot report yourself!");
            return;
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();

        reporter.sendMessage(ChatColor.GREEN + "You have reported " + ChatColor.WHITE + target.getName() +
                ChatColor.GREEN + " for: " + ChatColor.WHITE + reason);

        PlayerListener playerListener = new PlayerListener(plugin, tagsManager);
        playerListener.notifyStaff(reporter, target, reason);
    }
}