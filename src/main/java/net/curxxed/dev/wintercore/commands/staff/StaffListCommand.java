package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.menus.StaffListMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "stafflist",
        description = "View the staff list.",
        usage = "/stafflist",
        permission = "wintercore.stafflist",
        inGameOnly = true
)
public class StaffListCommand extends BaseCommand {

    public StaffListCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!player.hasPermission("wintercore.stafflist")
                && !player.hasPermission("wintercore.staff")
                && !player.hasPermission("wintercore.admin")
                && !player.hasPermission("wintercore.manager")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        new StaffListMenu(plugin).open(player);
    }
}