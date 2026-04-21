package net.curxxed.dev.wintercore.database.redis;

import com.google.gson.Gson;
import net.curxxed.dev.wintercore.database.redis.event.RedisPacketReceivedEvent;
import net.curxxed.dev.wintercore.database.redis.handler.BukkitRedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketCodec;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class RedisManager {

    public static final String CHANNEL = "wintercore:packets";

    private final WinterCore plugin;
    private final String serverName;
    private final RedisPacketCodec codec;
    private final BukkitRedisPacketHandler handler;

    public RedisManager(WinterCore plugin) {
        this.plugin = plugin;
        this.serverName = plugin.getConfig().getString("server-name", "Unknown");
        this.codec = new RedisPacketCodec(new Gson());
        this.handler = new BukkitRedisPacketHandler(plugin, plugin.getDisguiseEventListener());
    }

    public void start() {
        startHeartbeat();
        startListening();
    }

    public void stop() {
        stopHeartbeat();
    }

    public void publish(RedisPacket<?> packet) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish(CHANNEL, codec.encode(packet));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish Redis packet: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                String key = "server:" + serverName + ":heartbeat";
                jedis.set(key, "1");
                jedis.expire(key, 30);

                ServerInfoSnapshot snapshot = collectServerInfoSnapshot();
                String infoKey = "server:" + serverName + ":info";
                jedis.hset(infoKey, "tps", snapshot.tps);
                jedis.hset(infoKey, "players", String.valueOf(snapshot.players));
                jedis.hset(infoKey, "maxPlayers", String.valueOf(snapshot.maxPlayers));
                jedis.hset(infoKey, "whitelisted", String.valueOf(snapshot.whitelisted));
                jedis.hset(infoKey, "updatedAt", String.valueOf(System.currentTimeMillis()));
                jedis.expire(infoKey, 30);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to write heartbeat: " + e.getMessage());
            }
        }, 0L, 200L);
    }

    private void stopHeartbeat() {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.del("server:" + serverName + ":heartbeat");
            jedis.del("server:" + serverName + ":info");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove heartbeat: " + e.getMessage());
        }
    }

    private ServerInfoSnapshot collectServerInfoSnapshot() {
        try {
            if (Bukkit.isPrimaryThread()) {
                return buildServerInfoSnapshot();
            }
            return Bukkit.getScheduler().callSyncMethod(plugin, this::buildServerInfoSnapshot).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to collect server info snapshot: " + e.getMessage());
            return new ServerInfoSnapshot("0.00", 0, 0, false);
        }
    }

    private ServerInfoSnapshot buildServerInfoSnapshot() {
        int players = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        boolean whitelisted = Bukkit.hasWhitelist();

        double tpsValue = 20.0D;
        try {
            double[] tps = Utilities.getTPS();
            if (tps != null && tps.length > 0) {
                tpsValue = tps[0];
            }
        } catch (Exception ignored) {
        }

        if (Double.isNaN(tpsValue) || Double.isInfinite(tpsValue)) {
            tpsValue = 20.0D;
        }
        tpsValue = Math.min(20.0D, Math.max(0.0D, tpsValue));

        return new ServerInfoSnapshot(
                String.format(Locale.US, "%.2f", tpsValue),
                players,
                maxPlayers,
                whitelisted
        );
    }

    private static final class ServerInfoSnapshot {
        private final String tps;
        private final int players;
        private final int maxPlayers;
        private final boolean whitelisted;

        private ServerInfoSnapshot(String tps, int players, int maxPlayers, boolean whitelisted) {
            this.tps = tps;
            this.players = players;
            this.maxPlayers = maxPlayers;
            this.whitelisted = whitelisted;
        }
    }

    private void startListening() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = plugin.getRedisPool().getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (!CHANNEL.equals(channel)) {
                            return;
                        }

                        try {
                            final RedisPacket<RedisPacketHandler> packet =
                                    (RedisPacket<RedisPacketHandler>) codec.decode(message);

                            if (serverName.equalsIgnoreCase(packet.getSourceServer())) {
                                return;
                            }

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    Bukkit.getPluginManager().callEvent(new RedisPacketReceivedEvent(packet));
                                    packet.handle(handler);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to handle Redis packet: " + e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to decode Redis packet: " + e.getMessage());
                        }
                    }
                }, CHANNEL);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis subscription failed: " + e.getMessage());
            }
        });
    }

    public void publishAndHandleLocally(RedisPacket<RedisPacketHandler> packet) {
        publish(packet);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                packet.handle(handler);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to handle local packet: " + e.getMessage());
            }
        });
    }
}
