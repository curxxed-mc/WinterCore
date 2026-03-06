package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandInfo(
        name = "grant",
        permission = "WinterCore.commands.grant",
        description = "Open the rank selection GUI for target player.",
        aliases = {"setrank"},
        usage = "/grant <player>",
        inGameOnly = true
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
            sender.sendMessage(CC.translate("&cUsage: /grant <player>"));
            return;
        }

        String targetName = commandArgs.getArgs()[0];
        UUID targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();

        new RankMenu(plugin, targetUUID, targetName).open(sender);
    }
}