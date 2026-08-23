package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "grant",
        description = "Open the rank selection GUI for target player.",
        aliases = {"setrank"},
        usage = "/grant <player>",
        inGameOnly = true,
        permission = {"wintercore.commands.grant"}
)
public class GrantCommand extends BaseCommand {

    private final WinterCore plugin;

    public GrantCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player sender = commandArgs.getPlayer();

        if (commandArgs.length() < 1) {
            sendUsage(sender);
            return;
        }

        String targetName = commandArgs.getArgs()[0];
        resolvePlayerId(sender, targetName,
                targetUUID -> new RankMenu(plugin, targetUUID, targetName).open(sender));
    }
}
