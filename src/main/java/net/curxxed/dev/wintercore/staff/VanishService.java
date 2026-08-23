package net.curxxed.dev.wintercore.staff;

import net.curxxed.dev.wintercore.database.redis.packet.packets.VanishPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class VanishService implements Listener {

    private final WinterCore plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

    public VanishService(WinterCore plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return player != null && isVanished(player.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return uuid != null && vanishedPlayers.contains(uuid);
    }

    public void toggle(Player player, Consumer<Boolean> callback) {
        resolveColor(player, color -> plugin.getTasks().sync(
                () -> apply(player, !isVanished(player), color, callback)));
    }

    public void setVanished(Player player, boolean vanished, Consumer<Boolean> callback) {
        resolveColor(player, color -> plugin.getTasks().sync(
                () -> apply(player, vanished, color, callback)));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player viewer = event.getPlayer();
        if (isStaff(viewer)) {
            return;
        }
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null && vanished.isOnline()) {
                viewer.hidePlayer(vanished);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        vanishedPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void resolveColor(Player player, Consumer<String> callback) {
        if (player == null) {
            if (callback != null) {
                callback.accept("&f");
            }
            return;
        }
        plugin.getRankManager().getRank(player, rank ->
                plugin.getRankManager().getColorPreference(rank, callback));
    }

    private void apply(Player player, boolean vanished, String rankColor, Consumer<Boolean> callback) {
        if (player == null || !player.isOnline()) {
            complete(callback, false);
            return;
        }

        UUID uuid = player.getUniqueId();
        boolean changed = vanished ? vanishedPlayers.add(uuid) : vanishedPlayers.remove(uuid);
        if (!changed) {
            complete(callback, vanished);
            return;
        }

        if (vanished) {
            Utilities.getOnlinePlayers().stream()
                    .filter(viewer -> !isStaff(viewer))
                    .forEach(viewer -> viewer.hidePlayer(player));
            player.sendMessage(message("vanish.enabled", "&bYou are now vanished!"));
        } else {
            Utilities.getOnlinePlayers().forEach(viewer -> viewer.showPlayer(player));
            player.sendMessage(message("vanish.disabled", "&bYou are no longer vanished!"));
        }

        plugin.getRankManager().refreshPlayerDisplay(player);
        plugin.getRedisManager().publish(new VanishPacket(
                plugin.getConfig().getString("server-name", "Unknown"),
                System.currentTimeMillis(),
                uuid,
                player.getName(),
                vanished
        ));
        notifyStaff(player, rankColor, vanished);
        complete(callback, vanished);
    }

    private void notifyStaff(Player player, String rankColor, boolean vanished) {
        String notification = message(
                vanished ? "vanish.staff-enabled" : "vanish.staff-disabled",
                vanished ? "&9[S] {player} &bhas vanished!" : "&9[S] {player} &bhas reappeared!",
                "{player}", CC.translate(rankColor == null ? "&f" : rankColor) + player.getName()
        );
        Utilities.getOnlinePlayers().stream()
                .filter(this::isStaff)
                .filter(staff -> !staff.equals(player))
                .forEach(staff -> staff.sendMessage(notification));
    }

    private boolean isStaff(Player player) {
        return player.isOp()
                || player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager");
    }

    private void complete(Consumer<Boolean> callback, boolean vanished) {
        if (callback != null) {
            callback.accept(vanished);
        }
    }

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }
}
