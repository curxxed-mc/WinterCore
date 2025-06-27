package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == joining) continue;
            if (plugin.getDisguiseDataMap().containsKey(online.getUniqueId())) {
                String disguisedName = plugin.getDisguiseDataMap().get(online.getUniqueId()).getName();
                if (disguisedName != null && disguisedName.equalsIgnoreCase(joining.getName())) {
                    try {
                        disguiseHandler.undisguise(online);
                        plugin.getDisguiseRegistry().updateColorCache(online);
                        plugin.getNameTagHandler().updateNameTagFor(online);
                        online.kickPlayer("§cYour disguise was removed because the real player joined.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        plugin.getRedisManager().getDisguise(joining.getUniqueId(), disguiseJson -> {
            if (disguiseJson != null) {
                try {
                    JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    String rank = obj.get("rank").getAsString();
                    disguiseHandler.disguise(joining, rank, name, name);
                    plugin.getDisguiseRegistry().updateColorCache(joining);
                    plugin.getNameTagHandler().updateNameTagFor(joining);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                plugin.getDisguiseRegistry().updateColorCache(joining);
                plugin.getNameTagHandler().updateNameTagFor(joining);
            }
        });
    }

    @EventHandler
    public void onPlayerDisguise(net.curxxed.dev.wintercore.managers.events.PlayerDisguiseEvent event) {
        Player player = event.getPlayer();
        plugin.getDisguiseRegistry().updateColorCache(player);
        plugin.getNameTagHandler().updateNameTagFor(player);
    }

    @EventHandler
    public void onPlayerUnDisguise(net.curxxed.dev.wintercore.managers.events.PlayerUnDisguiseEvent event) {
        Player player = event.getPlayer();
        plugin.getDisguiseRegistry().updateColorCache(player);
        plugin.getNameTagHandler().updateNameTagFor(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);
        if (!isPending) {
            clearDisguiseOnDisconnect(player);
        } else {
            if (player.hasPermission("wintercore.disguise")) {
                String lastServer = plugin.getRedisManager().getLastServer(uuid);
                String currentServer = plugin.getConfig().getString("server-name", "unknown");
                plugin.getDisguiseRegistry().getEffectiveColor(player, color -> plugin.getRedisManager().publishDisguiseActivity(
                    "switch",
                    player.getName(),
                    color,
                    lastServer != null ? lastServer : "unknown",
                    currentServer
                ));
            }
        }
    }

    public void clearDisguiseOnDisconnect(Player player) {
        plugin.getDisguiseDataMap().remove(player.getUniqueId());
        plugin.getDisguiseRegistry().clear(player);
        plugin.getDisguiseRegistry().clearDisguiseInfo(player);
    }

    // Call this on server shutdown to undisguise all disguised players and clear Redis
    public void clearDisguiseOnShutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getDisguiseRegistry().isDisguised(player)) {
                try {
                    disguiseHandler.undisguise(player);
                    plugin.getDisguiseRegistry().clearDisguiseInfo(player); // Remove from Redis
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
