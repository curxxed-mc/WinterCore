package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.packet.packets.DisguiseStatePacket;
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
    public final Set<UUID> disguisedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> disguiseDataCache = new ConcurrentHashMap<>();

    private final RedisManager redisManager;
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
        if (player == null || skin == null) {
            return;
        }
        originalSkins.put(player.getUniqueId(), skin);
        disguisedPlayers.add(player.getUniqueId());
    }

    public void clear(Player player) {
        clear(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        originalSkins.remove(uuid);
        disguisedPlayers.remove(uuid);
        disguiseDataCache.remove(uuid);
        colorCache.remove(uuid);
    }

    public void clearAll(Consumer<Player> undisguiseAction) {
        for (UUID uuid : new HashSet<>(disguisedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                try {
                    undisguiseAction.accept(player);
                } catch (Exception ignored) {}
            }
        }
    }

    public void handleQuit(Player player, Consumer<Player> undisguiseAction) {
        UUID uuid = player.getUniqueId();
        scheduler.schedule(() -> {
            if (isDisguised(player)) {
                try {
                    undisguiseAction.accept(player);
                } catch (Exception e) {
                    logger.warning("Failed to undisguise player on quit: " + player.getName());
                }
                clear(uuid);
            }
        }, 4, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void publishDisguiseState(Player player, String disguiseName, String disguiseRank, String skin, String color, String prefix) {
        String serverName = WinterCore.getInstance().getConfig().getString("server-name", "Unknown");
        JsonObject obj = new JsonObject();
        obj.addProperty("name", disguiseName);
        obj.addProperty("rank", disguiseRank);
        obj.addProperty("skin", skin);
        obj.addProperty("color", color);
        obj.addProperty("prefix", prefix);
        obj.addProperty("server", serverName);
        String json = obj.toString();
        disguiseDataCache.put(player.getUniqueId(), json);

        try (redis.clients.jedis.Jedis jedis = WinterCore.getInstance().getRedisPool().getResource()) {
            jedis.setex("player:disguise:" + player.getUniqueId(), 7200, json);
        } catch (Exception e) {
            WinterCore.getInstance().getLogger().warning("Failed to persist disguise data: " + e.getMessage());
        }

        redisManager.publish(new DisguiseStatePacket(
                serverName,
                System.currentTimeMillis(),
                player.getUniqueId(),
                true,
                json
        ));
    }

    public void publishClearDisguise(Player player) {
        disguiseDataCache.remove(player.getUniqueId());

        try (redis.clients.jedis.Jedis jedis = WinterCore.getInstance().getRedisPool().getResource()) {
            jedis.del("player:disguise:" + player.getUniqueId());
        } catch (Exception e) {
            WinterCore.getInstance().getLogger().warning("Failed to clear disguise data: " + e.getMessage());
        }

        redisManager.publish(new DisguiseStatePacket(
                WinterCore.getInstance().getConfig().getString("server-name", "Unknown"),
                System.currentTimeMillis(),
                player.getUniqueId(),
                false,
                null
        ));
    }

    public String getDisguiseDataSync(UUID uuid) {
        String cached = disguiseDataCache.get(uuid);
        if (cached != null && !cached.isEmpty()) return cached;

        try (redis.clients.jedis.Jedis jedis = WinterCore.getInstance().getRedisPool().getResource()) {
            String persisted = jedis.get("player:disguise:" + uuid);
            if (persisted != null && !persisted.isEmpty()) {
                disguiseDataCache.put(uuid, persisted);
                return persisted;
            }
        } catch (Exception e) {
            WinterCore.getInstance().getLogger().warning("Failed to load disguise data: " + e.getMessage());
        }
        return "";
    }

    public void getEffectiveRank(Player player, Consumer<String> callback) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String rank = extractField(disguiseJson, "rank");
        if (rank != null) {
            callback.accept(rank);
            return;
        }
        RankManager.getInstance().getRank(player.getUniqueId(), callback);
    }

    public void getEffectiveColor(Player player, Consumer<String> callback) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String color = extractField(disguiseJson, "color");
        if (color != null) {
            callback.accept(color);
            return;
        }
        RankManager rankManager = RankManager.getInstance();
        rankManager.getRank(player.getUniqueId(), rank -> rankManager.getColorPreference(rank, callback));
    }

    public String getEffectiveColorSync(Player player) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String color = extractField(disguiseJson, "color");
        return color != null ? color : RankManager.getInstance().getColorPreferenceSync(player);
    }

    public void getEffectivePrefix(Player player, Consumer<String> callback) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String prefix = extractField(disguiseJson, "prefix");
        if (prefix != null) {
            callback.accept(prefix);
            return;
        }
        RankManager rankManager = RankManager.getInstance();
        rankManager.getRank(player.getUniqueId(), rank -> callback.accept(rankManager.getRankPrefixSync(rank)));
    }

    public void updateColorCache(Player player) {
        UUID uuid = player.getUniqueId();
        String color = null;
        if (disguisedPlayers.contains(uuid)) {
            String disguiseJson = disguiseDataCache.get(uuid);
            color = extractField(disguiseJson, "color");
        }
        if (color == null) {
            color = RankManager.getInstance().getColorPreferenceSync(player);
        }
        colorCache.put(uuid, color);
        final String resolvedColor = color;
        WinterCore instance = WinterCore.getInstance();
        if (instance == null) {
            return;
        }
        instance.getTasks().sync(() -> {
            if (instance != null && instance.getNameTagColorManager() != null) {
                instance.getNameTagColorManager().applyColor(player, resolvedColor);
            }
        });
    }

    public String getCachedColor(UUID uuid) {
        return colorCache.getOrDefault(uuid, "&f");
    }

    public void removeColorCache(UUID uuid) {
        colorCache.remove(uuid);
    }

    public void cacheDisguiseData(UUID uuid, String disguiseJson) {
        if (disguiseJson != null && !disguiseJson.isEmpty()) {
            disguiseDataCache.put(uuid, disguiseJson);
        }
    }

    private String extractField(String disguiseJson, String field) {
        if (disguiseJson == null) return null;
        try {
            JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
            if (obj.has(field) && !obj.get(field).isJsonNull() && !obj.get(field).getAsString().isEmpty()) {
                return obj.get(field).getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
