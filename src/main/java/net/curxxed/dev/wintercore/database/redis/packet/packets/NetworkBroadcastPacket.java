package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class NetworkBroadcastPacket extends AbstractRedisPacket {

    private final String sender;
    private final String message;

    public NetworkBroadcastPacket(String sourceServer, long timestamp, String sender, String message) {
        super(RedisPacketType.NETWORK_BROADCAST, sourceServer, timestamp);
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("sender", sender);
        json.addProperty("message", message);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}
