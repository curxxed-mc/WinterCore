package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.packet.packets.DisguiseStatePacket;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisguiseRegistry {

    private final Set<UUID> disguisedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<UUID, DisguiseData> activeDisguises = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> disguiseDataCache = new ConcurrentHashMap<>();

    private final WinterCore plugin;
    private final RedisManager redisManager;
    private final Logger logger;

    public DisguiseRegistry(WinterCore plugin) {
        this.plugin = plugin;
        this.redisManager = plugin.getRedisManager();
        this.logger = plugin.getLogger();
    }

    public boolean isDisguised(Player player) {
        return player != null && isDisguised(player.getUniqueId());
    }

    public boolean isDisguised(UUID uuid) {
        return uuid != null && disguisedPlayers.contains(uuid);
    }

    public boolean hasDisguiseData(UUID uuid) {
        return uuid != null && activeDisguises.containsKey(uuid);
    }

    public DisguiseData getDisguiseData(UUID uuid) {
        return uuid == null ? null : activeDisguises.get(uuid);
    }

    public void putDisguiseData(UUID uuid, DisguiseData data) {
        if (uuid != null && data != null) {
            activeDisguises.put(uuid, data);
        }
    }

    public DisguiseData removeDisguiseData(UUID uuid) {
        return uuid == null ? null : activeDisguises.remove(uuid);
    }

    public Map<UUID, DisguiseData> getDisguiseDataSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(activeDisguises));
    }

    public void setDisguised(Player player) {
        if (player == null) {
            return;
        }
        disguisedPlayers.add(player.getUniqueId());
    }

    public void clear(Player player) {
        clear(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        disguisedPlayers.remove(uuid);
        activeDisguises.remove(uuid);
        disguiseDataCache.remove(uuid);
        colorCache.remove(uuid);
    }

    public void publishDisguiseState(Player player, String disguiseName, String disguiseRank, String skin, String color, String prefix) {
        String serverName = plugin.getConfig().getString("server-name", "Unknown");
        JsonObject obj = new JsonObject();
        obj.addProperty("name", disguiseName);
        obj.addProperty("rank", disguiseRank);
        obj.addProperty("skin", skin);
        obj.addProperty("color", color);
        obj.addProperty("prefix", prefix);
        obj.addProperty("server", serverName);
        String json = obj.toString();
        disguiseDataCache.put(player.getUniqueId(), json);

        try (redis.clients.jedis.Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.setex("player:disguise:" + player.getUniqueId(), 7200, json);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to persist disguise data", e);
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

        try (redis.clients.jedis.Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.del("player:disguise:" + player.getUniqueId());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clear disguise data", e);
        }

        redisManager.publish(new DisguiseStatePacket(
                plugin.getConfig().getString("server-name", "Unknown"),
                System.currentTimeMillis(),
                player.getUniqueId(),
                false,
                null
        ));
    }

    public String getDisguiseDataSync(UUID uuid) {
        String cached = disguiseDataCache.get(uuid);
        if (cached != null && !cached.isEmpty()) return cached;

        try (redis.clients.jedis.Jedis jedis = plugin.getRedisPool().getResource()) {
            String persisted = jedis.get("player:disguise:" + uuid);
            if (persisted != null && !persisted.isEmpty()) {
                disguiseDataCache.put(uuid, persisted);
                return persisted;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load disguise data", e);
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
        plugin.getRankManager().getRank(player.getUniqueId(), callback);
    }

    public void getEffectiveColor(Player player, Consumer<String> callback) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String color = extractField(disguiseJson, "color");
        if (color != null) {
            callback.accept(color);
            return;
        }
        RankManager rankManager = plugin.getRankManager();
        rankManager.getRank(player.getUniqueId(), rank -> rankManager.getColorPreference(rank, callback));
    }

    public String getEffectiveColorSync(Player player) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String color = extractField(disguiseJson, "color");
        return color != null ? color : plugin.getRankManager().getColorPreferenceSync(player);
    }

    public void getEffectivePrefix(Player player, Consumer<String> callback) {
        String disguiseJson = disguiseDataCache.get(player.getUniqueId());
        String prefix = extractField(disguiseJson, "prefix");
        if (prefix != null) {
            callback.accept(prefix);
            return;
        }
        RankManager rankManager = plugin.getRankManager();
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
            color = plugin.getRankManager().getColorPreferenceSync(player);
        }
        colorCache.put(uuid, color);
        final String resolvedColor = color;
        plugin.getTasks().sync(() -> {
            if (plugin.getNameTagColorManager() != null) {
                plugin.getNameTagColorManager().applyColor(player, resolvedColor);
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

    @SuppressWarnings("deprecation") // Gson API available on the 1.8 server classpath.
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
