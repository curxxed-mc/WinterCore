package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand implements CommandExecutor {

    private final Main plugin;
    public static final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        if (!player.hasPermission("iCore.vanish")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }

        UUID playerId = player.getUniqueId();

        plugin.getRankManager().getRank(player, rank -> {
            plugin.getRankManager().getColorPreference(rank, rankColor -> {
                String playerRankColor = ChatColor.translateAlternateColorCodes('&', rankColor);

                if (vanishedPlayers.contains(playerId)) {
                    unVanishPlayer(player, playerRankColor);
                } else {
                    vanishPlayer(player, playerRankColor);
                }
            });
        });

        return true;
    }

    private void vanishPlayer(Player player, String rankColor) {
        vanishedPlayers.add(player.getUniqueId());

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> !(p.hasPermission("iCore.staff") || p.hasPermission("iCore.admin") || p.hasPermission("iCore.Manager")))
                .forEach(p -> p.hidePlayer(player));

        player.sendMessage(ChatColor.AQUA + "You are now vanished!");
        plugin.getRankManager().updatePlayerRank(player);

        sendStaffNotification(player, rankColor, true);
        plugin.getRedisManager().syncVanishState(player, true); // Redis support
    }

    private void unVanishPlayer(Player player, String rankColor) {
        vanishedPlayers.remove(player.getUniqueId());

        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));

        player.sendMessage(ChatColor.AQUA + "You are no longer vanished!");
        plugin.getRankManager().updatePlayerRank(player);

        sendStaffNotification(player, rankColor, false);
        plugin.getRedisManager().syncVanishState(player, false); // Redis support
    }

    private void sendStaffNotification(Player player, String rankColor, boolean vanished) {
        String messageTemplate = vanished
                ? plugin.getConfig().getString("StaffVanishMessages.vanish", "&9[S] %player% &bhas vanished!")
                : plugin.getConfig().getString("StaffVanishMessages.unvanish", "&9[S] %player% &bhas reappeared!");

        String message = ChatColor.translateAlternateColorCodes('&',
                messageTemplate.replace("%player%", rankColor + player.getName()));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("iCore.staff") || p.hasPermission("iCore.admin") || p.hasPermission("iCore.Manager"))
                .filter(p -> p != player)
                .forEach(staff -> staff.sendMessage(message));
    }
}
