package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.events.disguise.PlayerDisguiseEvent;
import net.curxxed.dev.wintercore.events.disguise.PlayerUnDisguiseEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseEventListener implements Listener {

    private final DefaultDisguiseHandler disguiseHandler;
    private final WinterCore plugin;
    private final Set<UUID> conflictEnforcement = ConcurrentHashMap.newKeySet();

    public DisguiseEventListener(WinterCore plugin, DefaultDisguiseHandler handler) {
        this.plugin = plugin;
        this.disguiseHandler = handler;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::auditNetworkDisguiseConflicts, 60L, 60L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        UUID uuid = joining.getUniqueId();

        String disguiseJson = plugin.getDisguiseRegistry().getDisguiseDataSync(uuid);

        if (disguiseJson != null && !disguiseJson.isEmpty()) {
            try {
                JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
                String name = obj.get("name").getAsString();
                String rank = obj.get("rank").getAsString();
                String skin = obj.has("skin") ? obj.get("skin").getAsString()
                        : (obj.has("skinName") ? obj.get("skinName").getAsString() : name);

                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        disguiseHandler.disguise(joining, rank, name, skin, result -> {
                            if (result == DisguiseCallback.GLOBAL_PLAYER_FOUND) {
                                handleInvalidRestoredDisguise(joining, name);
                                return;
                            }

                            if (result != DisguiseCallback.SUCCESS) {
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

        enforceLocalConflictsFor(joining.getName(), joining.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        conflictEnforcement.remove(uuid);
        plugin.getDisguiseDataMap().remove(uuid);
        plugin.getDisguiseRegistry().clear(player);
    }

    public void onServerSwitch(Player player) {
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
    }

    @EventHandler
    public void onPlayerUnDisguise(PlayerUnDisguiseEvent event) {
        plugin.getDisguiseRegistry().updateColorCache(event.getPlayer());
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
                plugin.getDisguiseRegistry().publishClearDisguise(player);
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

    private void auditNetworkDisguiseConflicts() {
        for (Map.Entry<UUID, DisguiseData> entry : plugin.getDisguiseDataMap().entrySet()) {
            UUID disguisedUuid = entry.getKey();
            DisguiseData data = entry.getValue();
            if (data == null || data.getName() == null || data.getName().trim().isEmpty()) {
                continue;
            }

            UUID ownerUuid = plugin.getNRS().getOnlineUuidByName(data.getName());
            if (ownerUuid == null || ownerUuid.equals(disguisedUuid)) {
                continue;
            }

            enqueueConflictEnforcement(disguisedUuid, data.getName());
        }
    }

    private void enforceLocalConflictsFor(String realName, UUID realUuid) {
        if (realName == null || realName.trim().isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, DisguiseData> entry : plugin.getDisguiseDataMap().entrySet()) {
            UUID disguisedUuid = entry.getKey();
            if (disguisedUuid.equals(realUuid)) {
                continue;
            }

            DisguiseData data = entry.getValue();
            if (data == null || data.getName() == null) {
                continue;
            }

            if (data.getName().equalsIgnoreCase(realName)) {
                enqueueConflictEnforcement(disguisedUuid, data.getName());
            }
        }
    }

    private void enqueueConflictEnforcement(UUID disguisedUuid, String conflictingName) {
        if (!conflictEnforcement.add(disguisedUuid)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> enforceConflictNow(disguisedUuid, conflictingName));
    }

    private void enforceConflictNow(UUID disguisedUuid, String conflictingName) {
        Player disguisedPlayer = Bukkit.getPlayer(disguisedUuid);
        if (disguisedPlayer == null || !disguisedPlayer.isOnline()) {
            conflictEnforcement.remove(disguisedUuid);
            return;
        }

        try {
            // Do not run packet-heavy undisguise here; forcing it right before kick can leave ghost entities.
            forceClearDisguiseState(disguisedPlayer);
            if (disguisedPlayer.isOnline()) {
                disguisedPlayer.kickPlayer(CC.translate(
                        "&cYour disguise was removed because &e" + conflictingName
                                + "&c is currently online on the network."
                ));
            }
        } finally {
            conflictEnforcement.remove(disguisedUuid);
        }
    }

    private void handleInvalidRestoredDisguise(Player player, String conflictingName) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            forceClearDisguiseState(player);
            if (player.isOnline()) {
                player.kickPlayer(CC.translate(
                        "&cYour saved disguise as &e" + conflictingName
                                + "&c is invalid because that player is now online on the network."
                ));
            }
        });
    }

    private void forceClearDisguiseState(Player player) {
        if (player == null) {
            return;
        }

        plugin.getDisguiseDataMap().remove(player.getUniqueId());
        plugin.getDisguiseRegistry().clear(player);
        plugin.getDisguiseRegistry().publishClearDisguise(player);

        if (plugin.getNameTagColorManager() != null) {
            plugin.getNameTagColorManager().clearDisguise(player);
            plugin.getNameTagColorManager().applyColor(
                    player,
                    plugin.getRankManager().getColorPreferenceSync(player)
            );
        }

        plugin.getRankManager().refreshPlayerDisplay(player);
    }
}
