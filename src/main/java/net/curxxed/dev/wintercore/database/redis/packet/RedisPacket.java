package net.curxxed.dev.wintercore.database.redis.packet;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;


public interface RedisPacket<T extends RedisPacketHandler> {
    RedisPacketType getType();

    JsonObject serialize();

    String getSourceServer();

    long getTimestamp();

    void handle(T handler);
}