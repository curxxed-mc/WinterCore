package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.database.redis.packet.packets.VanishPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@CommandInfo(
        name = "vanish",
        description = "Toggle vanish mode.",
        usage = "/vanish",
        inGameOnly = true,
        async = true,
        permission = {"wintercore.vanish", "WinterCore.vanish"}
)
public class VanishCommand extends BaseCommand {

    private final WinterCore plugin;
    public static final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        runSync(() -> {
            Player player = commandArgs.getPlayer();

            toggleVanish(player, plugin, (vanished) -> {
                ItemStack dye = player.getInventory().getItem(8);
                if (dye != null) {
                    ItemBuilder builder = new ItemBuilder(dye);
                    builder.setName(message(plugin,
                            vanished ? "vanish.item.unvanish" : "vanish.item.vanish",
                            vanished ? "&7Un-Vanish" : "&aVanish"));
                    player.getInventory().setItem(8, builder.toItemStack());
                }
            });
        });
    }

    public static void toggleVanish(Player player, WinterCore plugin, Consumer<Boolean> callback) {
        UUID playerId = player.getUniqueId();

        plugin.getRankManager().getRank(player, rank -> plugin.getRankManager().getColorPreference(rank, rankColor -> {
            plugin.getTasks().sync(() -> {
                String playerRankColor = CC.translate(rankColor);

                boolean nowVanished;

                if (vanishedPlayers.contains(playerId)) {
                    vanishedPlayers.remove(playerId);
                    net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().forEach(p -> p.showPlayer(player));
                    player.sendMessage(message(plugin, "vanish.disabled", "&bYou are no longer vanished!"));
                    plugin.getRankManager().refreshPlayerDisplay(player);
                    plugin.getRedisManager().publish(new VanishPacket(plugin.getConfig().getString("server-name", "Unknown"), System.currentTimeMillis(), player.getUniqueId(), player.getName(), false));
                    sendStaffNotificationStatic(player, playerRankColor, false, plugin);
                    nowVanished = false;
                } else {
                    vanishedPlayers.add(playerId);
                    net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                            .filter(p -> !(p.hasPermission("wintercore.staff") || p.hasPermission("wintercore.admin") || p.hasPermission("wintercore.Manager")))
                            .forEach(p -> p.hidePlayer(player));
                    player.sendMessage(message(plugin, "vanish.enabled", "&bYou are now vanished!"));
                    plugin.getRankManager().refreshPlayerDisplay(player);
                    plugin.getRedisManager().publish(new VanishPacket(plugin.getConfig().getString("server-name", "Unknown"), System.currentTimeMillis(), player.getUniqueId(), player.getName(), true));
                    sendStaffNotificationStatic(player, playerRankColor, true, plugin);
                    nowVanished = true;
                }

                callback.accept(nowVanished);
            });
        }));
    }

    private static void sendStaffNotificationStatic(Player player, String rankColor, boolean vanished, WinterCore plugin) {
        String message = message(plugin,
                vanished ? "vanish.staff-enabled" : "vanish.staff-disabled",
                vanished ? "&9[S] {player} &bhas vanished!" : "&9[S] {player} &bhas reappeared!",
                "{player}", rankColor + player.getName());

        net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("WinterCore.staff") || p.hasPermission("WinterCore.admin") || p.hasPermission("WinterCore.Manager"))
                .filter(p -> p != player)
                .forEach(staff -> staff.sendMessage(message));
    }

    public static boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    private static String message(WinterCore plugin, String path, String fallback, String... placeholders) {
        if (plugin != null && plugin.getMessageConfig() != null) {
            return plugin.getMessageConfig().get(path, fallback, placeholders);
        }

        String output = fallback;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return CC.translate(output);
    }
}
