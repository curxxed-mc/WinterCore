package net.curxxed.dev.wintercore.database.redis;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedisSocials {

    private final WinterCore plugin;

    public RedisSocials(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void setSocialLink(UUID uuid, String platform, String value) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hset(key(uuid), normalize(platform), value);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save social link for " + uuid + ": " + e.getMessage());
        }
    }

    public String getSocialLink(UUID uuid, String platform) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            return jedis.hget(key(uuid), normalize(platform));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load social link for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    public Map<String, String> getAllSocialLinks(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Map<String, String> map = jedis.hgetAll(key(uuid));
            return map == null ? new HashMap<>() : new HashMap<>(map);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load social links for " + uuid + ": " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void removeSocialLink(UUID uuid, String platform) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hdel(key(uuid), normalize(platform));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove social link for " + uuid + ": " + e.getMessage());
        }
    }

    private String key(UUID uuid) {
        return "social:" + uuid.toString();
    }

    private String normalize(String platform) {
        return platform.toLowerCase();
    }
}