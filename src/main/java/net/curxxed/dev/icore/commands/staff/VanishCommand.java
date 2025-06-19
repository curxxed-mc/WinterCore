package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class VanishCommand extends BaseCommand {

    private final iCore plugin;
    public static final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "vanish",
            permission = "iCore.vanish",
            description = "Toggle vanish mode.",
            usage = "/vanish",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        toggleVanish(player, plugin, (vanished) -> {
            ItemStack dye = player.getInventory().getItem(8);
            if (dye != null && dye.getItemMeta() != null) {
                ItemMeta meta = dye.getItemMeta();
                meta.setDisplayName(vanished ? ChatColor.GRAY + "Unvanish" : ChatColor.GREEN + "Vanish");
                dye.setItemMeta(meta);
                player.getInventory().setItem(8, dye);
            }
        });
    }

    public static void toggleVanish(Player player, iCore plugin, Consumer<Boolean> callback) {
        UUID playerId = player.getUniqueId();

        plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
            String playerRankColor = ChatColor.translateAlternateColorCodes('&', rankColor);

            boolean nowVanished;

            if (vanishedPlayers.contains(playerId)) {
                vanishedPlayers.remove(playerId);
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(player));
                player.sendMessage(ChatColor.AQUA + "You are no longer vanished!");
                plugin.getRankManager().refreshPlayerDisplay(player);
                plugin.getRedisManager().syncVanishState(player, false);
                sendStaffNotificationStatic(player, playerRankColor, false, plugin);
                nowVanished = false;
            } else {
                vanishedPlayers.add(playerId);
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !(p.hasPermission("icore.staff") || p.hasPermission("icore.admin") || p.hasPermission("icore.Manager")))
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

    private static void sendStaffNotificationStatic(Player player, String rankColor, boolean vanished, iCore plugin) {
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