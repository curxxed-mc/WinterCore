package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.menus.StaffListMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "stafflist",
        description = "View the staff list.",
        usage = "/stafflist",
        permission = {"wintercore.stafflist", "wintercore.staff", "wintercore.admin", "wintercore.manager"},
        inGameOnly = true
)
public class StaffListCommand extends BaseCommand {

    public StaffListCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (plugin.getNRS() == null) {
            player.sendMessage(CC.translate("&cThe staff list is currently unavailable. Please try again later."));
            return;
        }
        new StaffListMenu(plugin, plugin.getNRS()).open(player);
    }
}