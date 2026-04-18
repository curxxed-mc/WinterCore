package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class StaffActivityPacket extends AbstractRedisPacket {

    private final String activityType;
    private final String playerName;
    private final String color;
    private final String fromServer;
    private final String toServer;

    public StaffActivityPacket(String sourceServer, long timestamp, String activityType, String playerName, String color, String fromServer, String toServer) {
        super(RedisPacketType.STAFF_ACTIVITY, sourceServer, timestamp);
        this.activityType = activityType;
        this.playerName = playerName;
        this.color = color;
        this.fromServer = fromServer;
        this.toServer = toServer;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getColor() {
        return color;
    }

    public String getFromServer() {
        return fromServer;
    }

    public String getToServer() {
        return toServer;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("activityType", activityType);
        json.addProperty("playerName", playerName);
        json.addProperty("color", color);
        json.addProperty("fromServer", fromServer);
        json.addProperty("toServer", toServer);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}