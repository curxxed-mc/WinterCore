package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.managers.events.PlayerDisguiseEvent;
import net.curxxed.dev.wintercore.managers.events.PlayerUnDisguiseEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
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

        plugin.getLogger().info("[DisguiseDebug] Player " + joining.getName() + " joined. Checking Redis...");

        String disguiseJson = plugin.getRedisManager().getDisguiseSync(uuid);

        if (disguiseJson != null && !disguiseJson.isEmpty()) {
            plugin.getLogger().info("[DisguiseDebug] Data found: " + disguiseJson);
            try {
                JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
                String name = obj.get("name").getAsString();
                String rank = obj.get("rank").getAsString();
                String skin = obj.has("skinName") ? obj.get("skinName").getAsString() : name;

                // Delay execution to ensure player is fully in the world
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getLogger().info("[DisguiseDebug] Executing auto-disguise for " + joining.getName());

                    try {
                        // Matching your signature: disguise(Player player, String rank, String name, String skin)
                        DisguiseCallback result = disguiseHandler.disguise(joining, rank, name, skin);
                        plugin.getLogger().info("[DisguiseDebug] Auto-disguise result: " + result.name());
                    } catch (Exception e) {
                        plugin.getLogger().severe("[DisguiseDebug] Failed to execute disguise method");
                        e.printStackTrace();
                    }
                }, 2L);

            } catch (Exception e) {
                plugin.getLogger().severe("[DisguiseDebug] Error parsing redis-disguise JSON");
                e.printStackTrace();
            }
        }

        // Handle visibility for other disguised players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(joining)) continue;
            if (plugin.getDisguiseRegistry().isDisguised(online)) {
                refreshVisibility(online, joining);
            }
        }
    }

    /**
     * Re-sends packets so that 'viewer' sees 'disguised' with their disguised identity
     */
    private void refreshVisibility(Player disguised, Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        viewer.hidePlayer(disguised);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (viewer.isOnline() && disguised.isOnline()) {
                viewer.showPlayer(disguised);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearDisguiseOnDisconnect(event.getPlayer());
    }

    public void clearDisguiseOnDisconnect(Player player) {
        plugin.getDisguiseDataMap().remove(player.getUniqueId());
        plugin.getDisguiseRegistry().clear(player);
        plugin.getDisguiseRegistry().clearDisguiseInfo(player);
    }

    /**
     * Cleans up all disguises when the server shuts down or plugin disables.
     */
    public void clearDisguiseOnShutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDisguiseRegistry().isDisguised(player)) {
                try {
                    disguiseHandler.undisguise(player);
                    plugin.getDisguiseRegistry().clearDisguiseInfo(player);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to undisguise " + player.getName() + " on shutdown.");
                }
            }
        }
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
        plugin.getRedisManager().clearDisguise(event.getPlayer().getUniqueId());
        plugin.getDisguiseRegistry().updateColorCache(event.getPlayer());
        if (plugin.getNameTagHandler() != null) {
            plugin.getNameTagHandler().updateNameTagFor(event.getPlayer());
        }
    }
}