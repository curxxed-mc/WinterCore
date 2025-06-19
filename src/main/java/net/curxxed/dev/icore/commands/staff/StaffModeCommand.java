package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.staff.StaffModeManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StaffModeCommand extends BaseCommand {

    private final iCore plugin;
    private final StaffModeManager staffModeManager;

    public StaffModeCommand(iCore plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    @Command(
            name = "staffmode",
            description = "Toggle staff mode.",
            usage = "/staffmode",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!staffModeManager.hasStaffPermission(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }

        if (staffModeManager.isInStaffMode(player)) {
            staffModeManager.disableStaffMode(player);
        } else {
            staffModeManager.enableStaffMode(player);
        }
    }
}