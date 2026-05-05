package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "staffmode",
        description = "Toggle staff mode.",
        usage = "/staffmode",
        inGameOnly = true,
        permission = {}
)
public class StaffModeCommand extends BaseCommand {

    private final StaffModeManager staffModeManager;

    public StaffModeCommand(WinterCore plugin, StaffModeManager staffModeManager) {
        super(plugin);
        this.staffModeManager = staffModeManager;
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!staffModeManager.hasStaffPermission(player)) {
            send(player, "general.no-permission",
                    "&cYou don't have permission to use this command.");
            return;
        }

        if (staffModeManager.isInStaffMode(player)) {
            staffModeManager.disableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player);
        }
    }
}




