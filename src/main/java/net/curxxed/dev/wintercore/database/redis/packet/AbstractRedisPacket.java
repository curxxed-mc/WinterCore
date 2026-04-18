package net.curxxed.dev.wintercore.database.redis.packet;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;

public abstract class AbstractRedisPacket implements RedisPacket<RedisPacketHandler> {

    private final RedisPacketType type;
    private final String sourceServer;
    private final long timestamp;

    protected AbstractRedisPacket(RedisPacketType type, String sourceServer, long timestamp) {
        this.type = type;
        this.sourceServer = sourceServer;
        this.timestamp = timestamp;
    }

    @Override
    public RedisPacketType getType() {
        return type;
    }

    @Override
    public String getSourceServer() {
        return sourceServer;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    protected JsonObject baseJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.name());
        json.addProperty("sourceServer", sourceServer);
        json.addProperty("timestamp", timestamp);
        return json;
    }
}