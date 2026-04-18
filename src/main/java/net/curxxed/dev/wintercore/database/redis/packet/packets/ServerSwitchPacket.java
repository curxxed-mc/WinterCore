package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class ServerSwitchPacket extends AbstractRedisPacket {

    private final UUID uuid;
    private final String previousServer;
    private final String currentServer;

    public ServerSwitchPacket(String sourceServer, long timestamp, UUID uuid, String previousServer, String currentServer) {
        super(RedisPacketType.SERVER_SWITCH, sourceServer, timestamp);
        this.uuid = uuid;
        this.previousServer = previousServer;
        this.currentServer = currentServer;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPreviousServer() {
        return previousServer;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("previousServer", previousServer);
        json.addProperty("currentServer", currentServer);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}