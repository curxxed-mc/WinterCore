package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import curxxed.dev.icore.listeners.PlayerListener;
import curxxed.dev.icore.utils.RankManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManagerChatCommand implements CommandExecutor {
    private final Main plugin;
    private final PlayerListener playerListener;
    private final RankManager rankManager;

    public ManagerChatCommand(Main plugin, PlayerListener playerListener) {
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

        if (!player.hasPermission("iCore.Manager")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use manager chat.");
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Please provide a message.");
            return false;
        }

        String message = String.join(" ", args);

        playerListener.getManagerChatMessage(player, message, formattedMessage -> {
            if (formattedMessage != null) {
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("iCore.Manager"));

                plugin.getRedisManager().broadcastManagerMessage(formattedMessage);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use manager chat.");
            }
        });

        return true;
    }
}
