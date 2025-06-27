package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import net.curxxed.dev.wintercore.rank.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StaffChatCommand extends BaseCommand {
    private final WinterCore plugin;
    private final PlayerListener playerListener;
    private final RankManager rankManager;

    public StaffChatCommand(WinterCore plugin, PlayerListener playerListener) {
        this.plugin = plugin;
        this.rankManager = new RankManager(plugin);
        this.playerListener = playerListener;
    }

    @Command(
        name = "sc",
        permission = "WinterCore.staff",
        description = "Send a message to staff chat.",
        usage = "/staffchat <message>",
        inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        if (player == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        if (!(player.hasPermission("wintercore.staff") || player.hasPermission("wintercore.admin") || player.hasPermission("wintercore.manager"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Please provide a message.");
            return;
        }
        String message = String.join(" ", args);
        playerListener.getStaffChatMessage(player, message, formattedMessage -> {
            if (formattedMessage != null) {
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("WinterCore.staff") || p.hasPermission("WinterCore.Admin") || p.hasPermission("WinterCore.Manager"));
                plugin.getRedisManager().broadcastStaffMessage(formattedMessage);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            }
        });
    }
}
