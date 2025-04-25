package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

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

        toggleVanish(player, plugin, (vanished) -> {
            // Optionally update an item in hand or slot (for example slot 8)
            ItemStack dye = player.getInventory().getItem(8);
            if (dye != null && dye.getItemMeta() != null) {
                ItemMeta meta = dye.getItemMeta();
                meta.setDisplayName(vanished ? ChatColor.GRAY + "Unvanish" : ChatColor.GREEN + "Vanish");
                dye.setItemMeta(meta);
                player.getInventory().setItem(8, dye);
            }
        });

        return true;
    }

    public static void toggleVanish(Player player, Main plugin, Consumer<Boolean> callback) {
        UUID playerId = player.getUniqueId();

        plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
            String playerRankColor = ChatColor.translateAlternateColorCodes('&', rankColor);

            boolean nowVanished;

            if (vanishedPlayers.contains(playerId)) {
                // unvanish
                vanishedPlayers.remove(playerId);
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
                player.sendMessage(ChatColor.AQUA + "You are no longer vanished!");
                plugin.getRankManager().refreshPlayerDisplay(player);
                plugin.getRedisManager().syncVanishState(player, false);
                sendStaffNotificationStatic(player, playerRankColor, false, plugin);
                nowVanished = false;
            } else {
                // vanish
                vanishedPlayers.add(playerId);
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !(p.hasPermission("iCore.staff") || p.hasPermission("iCore.admin") || p.hasPermission("iCore.Manager")))
                        .forEach(p -> p.hidePlayer(player));
                player.sendMessage(ChatColor.AQUA + "You are now vanished!");
                plugin.getRankManager().refreshPlayerDisplay(player);
                plugin.getRedisManager().syncVanishState(player, true);
                sendStaffNotificationStatic(player, playerRankColor, true, plugin);
                nowVanished = true;
            }

            callback.accept(nowVanished);
        }));
    }

    private static void sendStaffNotificationStatic(Player player, String rankColor, boolean vanished, Main plugin) {
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

    public static boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }
}
