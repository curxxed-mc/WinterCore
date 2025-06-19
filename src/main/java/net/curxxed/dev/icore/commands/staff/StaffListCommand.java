package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.gui.StaffListGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StaffListCommand extends BaseCommand {

    private final iCore plugin;

    public StaffListCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "stafflist",
            description = "View the staff list.",
            usage = "/stafflist",
            permission = "icore.stafflist",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!(player.hasPermission("icore.stafflist") ||
                player.hasPermission("icore.staff") ||
                player.hasPermission("icore.admin") ||
                player.hasPermission("icore.manager"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        StaffListGUI gui = new StaffListGUI(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> gui.openGUI(player, plugin.getRedisManager()));
    }
}