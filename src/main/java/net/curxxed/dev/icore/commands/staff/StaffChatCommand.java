package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.listeners.PlayerListener;
import net.curxxed.dev.icore.rank.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class StaffChatCommand extends BaseCommand {
    private final iCore plugin;
    private final PlayerListener playerListener;
    private final RankManager rankManager;

    public StaffChatCommand(iCore plugin, PlayerListener playerListener) {
        this.plugin = plugin;
        this.rankManager = new RankManager(plugin);
        this.playerListener = playerListener;
    }

    @Command(
        name = "sc",
        permission = "iCore.staff",
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
        if (!(player.hasPermission("icore.staff") || player.hasPermission("icore.admin") || player.hasPermission("icore.manager"))) {
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
                        .filter(p -> p.hasPermission("iCore.staff") || p.hasPermission("iCore.Admin") || p.hasPermission("iCore.Manager"));
                plugin.getRedisManager().broadcastStaffMessage(formattedMessage);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            }
        });
    }
}
