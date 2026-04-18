package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class RankTagSyncPacket extends AbstractRedisPacket {

    public enum SyncType {
        RANKS,
        TAGS,
        ALL
    }

    private final SyncType syncType;
    private final String yaml;

    public RankTagSyncPacket(String sourceServer, long timestamp, SyncType syncType, String yaml) {
        super(RedisPacketType.RANK_TAG_SYNC, sourceServer, timestamp);
        this.syncType = syncType;
        this.yaml = yaml;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public String getYaml() {
        return yaml;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("syncType", syncType.name());
        json.addProperty("yaml", yaml);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}