package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class PlayerReportPacket extends AbstractRedisPacket {

    private final String reporter;
    private final String reported;
    private final String reason;
    private final String server;

    public PlayerReportPacket(String sourceServer, long timestamp, String reporter, String reported, String reason, String server) {
        super(RedisPacketType.PLAYER_REPORT, sourceServer, timestamp);
        this.reporter = reporter;
        this.reported = reported;
        this.reason = reason;
        this.server = server;
    }

    public String getReporter() {
        return reporter;
    }

    public String getReported() {
        return reported;
    }

    public String getReason() {
        return reason;
    }

    public String getServer() {
        return server;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("reporter", reporter);
        json.addProperty("reported", reported);
        json.addProperty("reason", reason);
        json.addProperty("server", server);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}