package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.menus.StaffListMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StaffListCommand extends BaseCommand {

    private final WinterCore plugin;

    public StaffListCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "stafflist",
            description = "View the staff list.",
            usage = "/stafflist",
            permission = "wintercore.stafflist",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!(player.hasPermission("wintercore.stafflist") ||
                player.hasPermission("wintercore.staff") ||
                player.hasPermission("wintercore.admin") ||
                player.hasPermission("wintercore.manager"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        StaffListMenu gui = new StaffListMenu(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> gui.openGUI(player, plugin.getRedisManager()));
    }
}