package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class ServerStatusPacket extends AbstractRedisPacket {

    private final boolean online;

    public ServerStatusPacket(String sourceServer, long timestamp, boolean online) {
        super(RedisPacketType.SERVER_STATUS, sourceServer, timestamp);
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("online", online);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}