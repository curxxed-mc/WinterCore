package net.curxxed.dev.wintercore.database.redis.service;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;
import java.util.Locale;
import java.util.UUID;

public final class NetworkRedisService {

    private final WinterCore plugin;
    private static final int ONLINE_PRESENCE_TTL_SECONDS = 30;

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

    public void setStaffLastSeen(UUID uuid, long timestamp) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.hset("staff:last-seen", uuid.toString(), String.valueOf(timestamp));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set staff last seen for " + uuid + ": " + e.getMessage());
        }
    }

    public long getStaffLastSeen(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String val = jedis.hget("staff:last-seen", uuid.toString());
            return val != null ? Long.parseLong(val) : 0L;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get staff last seen for " + uuid + ": " + e.getMessage());
            return 0L;
        }
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

    public void setSwitching(UUID uuid, String serverName) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            // Set the server they are leaving, and expire it after 3 seconds
            jedis.setex("staff:switching:" + uuid.toString(), 3, serverName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set switching flag: " + e.getMessage());
        }
    }

    public String getAndRemoveSwitching(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String key = "staff:switching:" + uuid.toString();
            String previousServer = jedis.get(key);
            if (previousServer != null) {
                jedis.del(key); // Remove it so the quit task knows to abort
            }
            return previousServer;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isStillSwitching(UUID uuid) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            return jedis.exists("staff:switching:" + uuid.toString());
        } catch (Exception e) {
            return false;
        }
    }

    public void setOnlinePresence(UUID uuid, String playerName, String serverName) {
        if (uuid == null || playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return;
        }

        String uuidKey = "player:online:uuid:" + uuid;
        String nameKey = "player:online:name:" + normalizedName;
        String payload = playerName + "|" + (serverName == null ? "unknown" : serverName);

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Pipeline pipeline = jedis.pipelined();
            pipeline.setex(uuidKey, ONLINE_PRESENCE_TTL_SECONDS, payload);
            pipeline.setex(nameKey, ONLINE_PRESENCE_TTL_SECONDS, uuid.toString());
            pipeline.sync();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set online presence for " + playerName + ": " + e.getMessage());
        }
    }

    public void clearOnlinePresence(UUID uuid, String playerName) {
        if (uuid == null || playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return;
        }

        String uuidKey = "player:online:uuid:" + uuid;
        String nameKey = "player:online:name:" + normalizedName;

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            Pipeline pipeline = jedis.pipelined();
            pipeline.del(uuidKey);
            pipeline.del(nameKey);
            pipeline.sync();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to clear online presence for " + playerName + ": " + e.getMessage());
        }
    }

    public UUID getOnlineUuidByName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return null;
        }

        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            String raw = jedis.get("player:online:name:" + normalizedName);
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isNameOnlineElsewhere(String playerName, UUID excludingUuid) {
        UUID onlineUuid = getOnlineUuidByName(playerName);
        return onlineUuid != null && (excludingUuid == null || !onlineUuid.equals(excludingUuid));
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ENGLISH);
    }
}
