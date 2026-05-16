package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.menus.StaffListMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "stafflist",
        description = "View the staff list.",
        usage = "/stafflist",
        inGameOnly = true,
        permission = {"wintercore.stafflist", "wintercore.staff", "wintercore.admin", "wintercore.manager"}
)
public class StaffListCommand extends BaseCommand {

    public StaffListCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (plugin.getNRS() == null) {
            send(player, "staff-list.unavailable",
                    "&cThe staff list is currently unavailable. Please try again later.");
            return;
        }
        new StaffListMenu(plugin, plugin.getNRS()).open(player);
    }
}
