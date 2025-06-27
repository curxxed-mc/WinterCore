package net.curxxed.dev.wintercore.disguise;

import net.curxxed.dev.wintercore.database.RedisManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DisguiseRegistry {
    private final Map<UUID, SkinFetcher.SkinProperty> originalSkins = new ConcurrentHashMap<>();
    private final Set<UUID> disguisedPlayers = ConcurrentHashMap.newKeySet();
    private final RedisManager redisManager;
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();

    public DisguiseRegistry(RedisManager redisManager, Logger logger) {
        this.redisManager = redisManager;
        this.logger = logger;
    }

    public boolean isDisguised(Player player) {
        return disguisedPlayers.contains(player.getUniqueId());
    }

    public SkinFetcher.SkinProperty getOriginalSkin(Player player) {
        return originalSkins.get(player.getUniqueId());
    }

    public void setDisguised(Player player, SkinFetcher.SkinProperty skin) {
        originalSkins.put(player.getUniqueId(), skin);
        disguisedPlayers.add(player.getUniqueId());
    }

    public void clear(Player player) {
        UUID uuid = player.getUniqueId();
        originalSkins.remove(uuid);
        disguisedPlayers.remove(uuid);
    }

    public void clearAll(Consumer<Player> undisguiseAction) {
        for (UUID uuid : new HashSet<>(disguisedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                try {
                    undisguiseAction.accept(player);
                } catch (Exception e) {
                    logger.warning("Failed to undisguise player on shutdown: " + player.getName());
                }
            }
        }
    }

    public void clear(UUID uuid) {
        originalSkins.remove(uuid);
        disguisedPlayers.remove(uuid);
    }

    /**
     * Call this on PlayerQuitEvent. Will only clear disguise if not switching servers.
     * Schedules permissions delayed cleanup to allow for cross-server reconnects.
     */
    public void handleQuit(Player player, Consumer<Player> undisguiseAction) {
        UUID uuid = player.getUniqueId();
        scheduler.schedule(() -> {
            if (!redisManager.isStillPendingSwitch(uuid)) {
                if (isDisguised(player)) {
                    try {
                        undisguiseAction.accept(player);
                    } catch (Exception e) {
                        logger.warning("Failed to undisguise player on quit: " + player.getName());
                    }
                    clear(player);
                }
            }
        }, 4, TimeUnit.SECONDS); // 4s delay for cross-server
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Store disguise info in Redis for permissions player.
     */
    public void setDisguiseInfo(Player player, String disguiseName, String disguiseRank, String color, String prefix) {
        // Build JSON for disguise info
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("name", disguiseName);
        obj.addProperty("rank", disguiseRank);
        obj.addProperty("color", color);
        obj.addProperty("prefix", prefix);
        redisManager.setDisguise(player.getUniqueId(), obj.toString());
    }

    /**
     * Remove disguise info from Redis for permissions player.
     */
    public void clearDisguiseInfo(Player player) {
        redisManager.clearDisguise(player.getUniqueId());
    }

    /**
     * Get the effective rank for permissions player (disguise rank if present, else real rank).
     * This is async and returns via callback.
     */
    public void getEffectiveRank(Player player, java.util.function.Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            if (disguiseJson != null) {
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(disguiseJson).getAsJsonObject();
                    if (obj.has("rank") && !obj.get("rank").isJsonNull()) {
                        callback.accept(obj.get("rank").getAsString());
                        return;
                    }
                } catch (Exception ignore) {}
            }
            // Fallback to real rank
            RankManager rankManager = RankManager.getInstance();
            rankManager.getRank(player, callback);
        });
    }

    /**
     * Get the effective color for permissions player (disguise color if present, else real color).
     * This is async and returns via callback.
     */
    public void getEffectiveColor(Player player, java.util.function.Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            if (disguiseJson != null) {
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(disguiseJson).getAsJsonObject();
                    if (obj.has("color") && !obj.get("color").isJsonNull()) {
                        callback.accept(obj.get("color").getAsString());
                        return;
                    }
                } catch (Exception ignore) {}
            }
            // Fallback to real color (fully async, no getRankSync)
            RankManager rankManager = RankManager.getInstance();
            rankManager.getRankAsync(player, realRank -> rankManager.getColorPreference(realRank, callback));
        });
    }

    /**
     * Get the effective color for permissions player (disguise color if present, else real color).
     * This is synchronous and returns the color directly.
     */
    public String getEffectiveColorSync(Player player) {
        String disguiseJson = redisManager.getDisguiseSync(player.getUniqueId());
        if (disguiseJson != null) {
            try {
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(disguiseJson).getAsJsonObject();
                if (obj.has("color") && !obj.get("color").isJsonNull()) {
                    return obj.get("color").getAsString();
                }
            } catch (Exception ignore) {}
        }
        // Fallback to real color
        return RankManager.getInstance().getColorPreferenceSync(player);
    }

    /**
     * Get the effective prefix for permissions player (disguise prefix if present, else real prefix).
     * This is async and returns via callback.
     */
    public void getEffectivePrefix(Player player, java.util.function.Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            if (disguiseJson != null) {
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(disguiseJson).getAsJsonObject();
                    if (obj.has("prefix") && !obj.get("prefix").isJsonNull()) {
                        callback.accept(obj.get("prefix").getAsString());
                        return;
                    }
                } catch (Exception ignore) {}
            }
            // Fallback to real prefix
            RankManager rankManager = RankManager.getInstance();
            rankManager.getRankPrefix(player, callback);
        });
    }

    public void updateColorCache(Player player) {
        UUID uuid = player.getUniqueId();
        redisManager.getDisguise(uuid, disguiseJson -> {
            String color = null;
            if (disguiseJson != null) {
                try {
                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(disguiseJson).getAsJsonObject();
                    if (obj.has("color") && !obj.get("color").isJsonNull()) {
                        color = obj.get("color").getAsString();
                    }
                } catch (Exception ignore) {}
            }
            if (color == null) {
                // Fallback to real color (sync is fine here, since it's cached in RankManager)
                color = RankManager.getInstance().getColorPreferenceSync(player);
            }
            colorCache.put(uuid, color);
            // Update scoreboard for this player for all viewers
            Bukkit.getScheduler().runTask(WinterCore.INSTANCE, () -> {
                WinterCore.INSTANCE.getNameTagHandler().updateNameTagFor(player);
            });
        });
    }

    public String getCachedColor(UUID uuid) {
        return colorCache.getOrDefault(uuid, "&f");
    }

    public void removeColorCache(UUID uuid) {
        colorCache.remove(uuid);
    }
}
