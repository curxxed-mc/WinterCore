package net.curxxed.dev.wintercore.database.redis.service;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

public final class NetworkRedisService {

    private final WinterCore plugin;

    public NetworkRedisService(WinterCore plugin) {
        this.plugin = plugin;
    }

    public Map<String, String> getStaffLastServers() {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Map<String, String> result = jedis.hgetAll("staff:last-server");
            return result != null ? new HashMap<>(result) : Collections.emptyMap();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get staff last servers: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Set<String> getAliveServers() {
        Set<String> alive = new HashSet<>();
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String cursor = "0";
            ScanParams params = new ScanParams().match("server:*:heartbeat").count(100);
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                for (String key : result.getResult()) {
                    String[] parts = key.split(":");
                    if (parts.length == 3) {
                        alive.add(parts[1]);
                    }
                }
                cursor = result.getCursor();
            } while (!cursor.equals("0"));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to scan alive servers: " + e.getMessage());
        }
        return alive;
    }

    public void setStaffLastServer(UUID uuid, String server) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hset("staff:last-server", uuid.toString(), server);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set staff last server for " + uuid + ": " + e.getMessage());
        }
    }

    public void removeStaffLastServer(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hdel("staff:last-server", uuid.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove staff last server for " + uuid + ": " + e.getMessage());
        }
    }

    public void cacheUsername(UUID uuid, String name) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.set("username:" + uuid.toString(), name);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache username for " + uuid + ": " + e.getMessage());
        }
    }

    public String getCachedUsername(String uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            return jedis.get("username:" + uuid);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get cached username for " + uuid + ": " + e.getMessage());
            return null;
        }
    }
}