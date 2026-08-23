package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class RemoteCommandPacket extends AbstractRedisPacket {

    private final String targetServer;
    private final String command;

    public RemoteCommandPacket(String sourceServer, long timestamp, String targetServer, String command) {
        super(RedisPacketType.REMOTE_COMMAND, sourceServer, timestamp);
        this.targetServer = targetServer;
        this.command = command;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("targetServer", targetServer);
        json.addProperty("command", command);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}