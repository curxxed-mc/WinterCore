package net.curxxed.dev.icore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.disguise.impl.DefaultDisguiseHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

public class DisguiseEventListener implements Listener {
    private final DefaultDisguiseHandler disguiseHandler;
    private final iCore plugin;

    public DisguiseEventListener(iCore plugin, DefaultDisguiseHandler handler) {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
       // plugin.getRedisManager().clearPendingSwitch(joining.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean isPending = plugin.getRedisManager().isStillPendingSwitch(uuid);
        if (!isPending) {
            plugin.getDisguiseDataMap().remove(uuid);
            plugin.getDisguiseRegistry().clear(player);
            plugin.getDisguiseRegistry().clearDisguiseInfo(player);
        } else {
            if (player.hasPermission("icore.disguise")) {
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
}
