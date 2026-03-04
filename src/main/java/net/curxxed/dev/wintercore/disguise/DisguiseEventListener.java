package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.events.PlayerDisguiseEvent;
import net.curxxed.dev.wintercore.events.PlayerUnDisguiseEvent;
import net.curxxed.dev.wintercore.events.ServerSwitchEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class DisguiseEventListener implements Listener {

    private final DefaultDisguiseHandler disguiseHandler;
    private final WinterCore plugin;

    public DisguiseEventListener(WinterCore plugin, DefaultDisguiseHandler handler) {
        this.plugin = plugin;
        this.disguiseHandler = handler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        UUID uuid = joining.getUniqueId();

        String disguiseJson = plugin.getRedisManager().getDisguiseSync(uuid);

        if (disguiseJson != null && !disguiseJson.isEmpty()) {
            try {
                JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
                String name = obj.get("name").getAsString();
                String rank = obj.get("rank").getAsString();
                String skin = obj.has("skinName") ? obj.get("skinName").getAsString() : name;

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        disguiseHandler.disguise(joining, rank, name, skin, result -> {
                            if (result != net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback.SUCCESS) {
                                plugin.getLogger().warning("[Disguise] Failed to restore disguise for "
                                        + joining.getName() + " on join (callback=" + result + ")");
                            }
                        }), 2L);

            } catch (Exception e) {
                plugin.getLogger().severe("[Disguise] Error parsing Redis disguise JSON for " + joining.getName());
                e.printStackTrace();
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(joining)) continue;
            if (plugin.getDisguiseRegistry().isDisguised(online)) {
                refreshVisibilityFor(online, joining);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean isPendingSwitch = plugin.getRedisManager().isStillPendingSwitch(uuid);

        plugin.getDisguiseDataMap().remove(uuid);
        plugin.getDisguiseRegistry().clear(player);

        if (!isPendingSwitch) {
            plugin.getDisguiseRegistry().clearDisguiseInfo(player);
        }
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getDisguiseRegistry().isDisguised(player)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                refreshVisibilityFor(player, online);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerDisguise(PlayerDisguiseEvent event) {
        plugin.getDisguiseRegistry().updateColorCache(event.getPlayer());
        if (plugin.getNameTagHandler() != null) {
            plugin.getNameTagHandler().updateNameTagFor(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerUnDisguise(PlayerUnDisguiseEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getRedisManager().isStillPendingSwitch(uuid)) {
            plugin.getRedisManager().clearDisguise(uuid);
        }

        plugin.getDisguiseRegistry().updateColorCache(player);
        if (plugin.getNameTagHandler() != null) {
            plugin.getNameTagHandler().updateNameTagFor(player);
        }
    }

    public void clearDisguiseOnShutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDisguiseRegistry().isDisguised(player)) {
                disguiseHandler.undisguise(player, result -> {
                    if (result != net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback.SUCCESS) {
                        plugin.getLogger().warning("[Disguise] Failed to undisguise "
                                + player.getName() + " on shutdown.");
                    }
                });
                plugin.getDisguiseRegistry().clearDisguiseInfo(player);
            }
        }
    }

    private void refreshVisibilityFor(Player disguised, Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        viewer.hidePlayer(disguised);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline() && disguised.isOnline()) {
                viewer.showPlayer(disguised);
            }
        }, 1L);
    }
}