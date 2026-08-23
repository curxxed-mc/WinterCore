package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class RankTagSyncPacket extends AbstractRedisPacket {

    private final UUID targetUuid;
    private final String rank;

    public RankTagSyncPacket(String sourceServer, long timestamp, UUID targetUuid, String rank) {
        super(RedisPacketType.RANK_TAG_SYNC, sourceServer, timestamp);
        this.targetUuid = targetUuid;
        this.rank = rank;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getRank() {
        return rank;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("targetUuid", targetUuid.toString());
        json.addProperty("rank", rank);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}
