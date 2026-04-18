package net.curxxed.dev.wintercore.database.redis;

import com.google.gson.Gson;
import net.curxxed.dev.wintercore.database.redis.event.RedisPacketReceivedEvent;
import net.curxxed.dev.wintercore.database.redis.handler.BukkitRedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketCodec;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

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
        this.handler = new BukkitRedisPacketHandler(plugin);
    }

    public void start() {
        startListening();
    }

    public void publish(RedisPacket<BukkitRedisPacketHandler> packet) {
        try (Jedis jedis = plugin.getRedisPool().getResource()) {
            jedis.publish(CHANNEL, codec.encode(packet));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to publish Redis packet: " + e.getMessage());
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
                            final RedisPacket<RedisPacketHandler> packet = (RedisPacket<RedisPacketHandler>) codec.decode(message);

                            if (serverName.equalsIgnoreCase(packet.getSourceServer())) {
                                return;
                            }

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Bukkit.getPluginManager().callEvent(new RedisPacketReceivedEvent(packet));
                                packet.handle(handler);
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
}