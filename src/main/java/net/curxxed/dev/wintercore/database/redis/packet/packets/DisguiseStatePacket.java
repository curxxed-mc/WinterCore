package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class DisguiseStatePacket extends AbstractRedisPacket {

    private final UUID uuid;
    private final boolean disguised;
    private final String disguiseJson;

    public DisguiseStatePacket(String sourceServer, long timestamp, UUID uuid, boolean disguised, String disguiseJson) {
        super(RedisPacketType.DISGUISE_STATE, sourceServer, timestamp);
        this.uuid = uuid;
        this.disguised = disguised;
        this.disguiseJson = disguiseJson;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDisguised() {
        return disguised;
    }

    public String getDisguiseJson() {
        return disguiseJson;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("disguised", disguised);
        json.addProperty("disguiseJson", disguiseJson);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}