package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class VanishPacket extends AbstractRedisPacket {

    private final UUID playerUuid;
    private final String playerName;
    private final boolean vanished;

    public VanishPacket(String sourceServer, long timestamp, UUID playerUuid, String playerName, boolean vanished) {
        super(RedisPacketType.VANISH_STATE, sourceServer, timestamp);
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.vanished = vanished;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isVanished() {
        return vanished;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("playerUuid", playerUuid.toString());
        json.addProperty("playerName", playerName);
        json.addProperty("vanished", vanished);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}

