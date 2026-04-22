package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

import java.util.UUID;

public final class ModerationActionPacket extends AbstractRedisPacket {

    public enum ActionType {
        BAN_APPLIED,
        BAN_REMOVED
    }

    private final ActionType actionType;
    private final UUID targetUuid;
    private final String targetName;
    private final String issuer;
    private final String reason;
    private final Long expiresAt;
    private final boolean silent;

    public ModerationActionPacket(String sourceServer, long timestamp, ActionType actionType, UUID targetUuid, String targetName, String issuer, String reason, Long expiresAt, boolean silent) {
        super(RedisPacketType.MODERATION_ACTION, sourceServer, timestamp);
        this.actionType = actionType;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.issuer = issuer;
        this.reason = reason;
        this.expiresAt = expiresAt;
        this.silent = silent;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getReason() {
        return reason;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public boolean isSilent() {
        return silent;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("actionType", actionType.name());
        json.addProperty("targetUuid", targetUuid.toString());
        json.addProperty("targetName", targetName);
        json.addProperty("issuer", issuer);
        json.addProperty("reason", reason);
        if (expiresAt == null) {
            json.add("expiresAt", JsonNull.INSTANCE);
        } else {
            json.addProperty("expiresAt", expiresAt);
        }
        json.addProperty("silent", silent);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}
