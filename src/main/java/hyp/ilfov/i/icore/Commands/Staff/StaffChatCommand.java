package hyp.ilfov.i.icore.Commands.Staff;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.listeners.PlayerListener;
import hyp.ilfov.i.icore.utils.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {
    private final Main plugin;
    private final PlayerListener playerListener;
    private final RankManager rankManager;

    public StaffChatCommand(Main plugin, PlayerListener playerListener) {
        this.plugin = plugin;
        this.rankManager = new RankManager(plugin);
        this.playerListener = playerListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        if (!(player.hasPermission("iCore.Staff") || player.hasPermission("iCore.Admin") || player.hasPermission("iCore.Manager"))) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Please provide a message.");
            return false;
        }

        String message = String.join(" ", args);

        playerListener.getStaffChatMessage(player, message, formattedMessage -> {
            if (formattedMessage != null) {
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("iCore.Staff") || p.hasPermission("iCore.Admin") || p.hasPermission("iCore.Manager"));

                plugin.getRedisManager().broadcastStaffMessage(formattedMessage);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use staff chat.");
            }
        });

        return true;
    }
}
