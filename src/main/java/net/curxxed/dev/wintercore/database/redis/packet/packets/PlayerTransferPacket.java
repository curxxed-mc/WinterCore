package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class PlayerTransferPacket extends AbstractRedisPacket {

    private final UUID targetUuid;
    private final String targetName;
    private final String destinationServer;
    private final String issuer;

    public PlayerTransferPacket(String sourceServer, long timestamp, UUID targetUuid,
                                String targetName, String destinationServer, String issuer) {
        super(RedisPacketType.PLAYER_TRANSFER, sourceServer, timestamp);
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.destinationServer = destinationServer;
        this.issuer = issuer;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDestinationServer() {
        return destinationServer;
    }

    public String getIssuer() {
        return issuer;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("targetUuid", targetUuid.toString());
        json.addProperty("targetName", targetName);
        json.addProperty("destinationServer", destinationServer);
        json.addProperty("issuer", issuer);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}
