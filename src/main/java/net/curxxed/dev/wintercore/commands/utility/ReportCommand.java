package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.tags.TagsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "report",
        description = "Report a player to the staff.",
        usage = "/report <player> <reason>",
        inGameOnly = true,
        permission = {}
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
            sendUsage(reporter);
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null || !target.isOnline()) {
            send(reporter, "moderation.report.player-not-found",
                    "&cPlayer not found or not online.");
            return;
        }

        if (target.equals(reporter)) {
            send(reporter, "moderation.report.self-report",
                    "&cYou cannot report yourself!");
            return;
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();

        send(reporter, "moderation.report.submitted",
                "&aYou have reported &f{target}&a for: &f{reason}",
                "{target}", target.getName(),
                "{reason}", reason);

        plugin.getPlayerService().sendReport(reporter, target, reason);
    }
}





