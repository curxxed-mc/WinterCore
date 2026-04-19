package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.tags.TagsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "report",
            description = "Report a player to the staff.",
            usage = "/report <player> <reason>",
            inGameOnly = true
    
    )
public class ReportCommand extends BaseCommand {
    private final WinterCore plugin;
    private final TagsManager tagsManager;

    public ReportCommand(WinterCore plugin, TagsManager tagsManager) {
        super(plugin);
        this.plugin = plugin;
        this.tagsManager = tagsManager;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
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

        PlayerService playerService = new PlayerService(plugin);
        playerService.sendReport(reporter, target, reason);
    }
}