package net.curxxed.dev.wintercore.database.redis;

import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;

public interface RedisPacketListener {
    void onPacket(RedisPacket<?> packet);
}
