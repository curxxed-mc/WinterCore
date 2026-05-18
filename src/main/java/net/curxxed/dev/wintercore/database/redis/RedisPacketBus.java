package net.curxxed.dev.wintercore.database.redis;

import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import net.curxxed.dev.wintercore.plugin.WinterCore;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class RedisPacketBus {

    private final WinterCore plugin;
    private final Set<RedisPacketListener> listeners = new CopyOnWriteArraySet<>();

    RedisPacketBus(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void subscribe(RedisPacketListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(RedisPacketListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    void notifyListeners(RedisPacket<?> packet) {
        for (RedisPacketListener listener : listeners) {
            try {
                listener.onPacket(packet);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis packet listener failed: " + e.getMessage());
            }
        }
    }
}
