package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public class PlayerUpdatePacket extends AbstractRedisPacket {

    private final UUID targetUuid;

    public PlayerUpdatePacket(String sourceServer, long timestamp, UUID targetUuid) {
        super(RedisPacketType.PLAYER_UPDATE, sourceServer, timestamp);
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("targetUuid", targetUuid.toString());
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}