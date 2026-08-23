package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class ChatBroadcastPacket extends AbstractRedisPacket {

    public enum ChatType { STAFF, ADMIN, MANAGER }

    private final ChatType chatType;
    private final String message;

    public ChatBroadcastPacket(String sourceServer, long timestamp, ChatType chatType, String message) {
        super(RedisPacketType.CHAT_BROADCAST, sourceServer, timestamp);
        this.chatType = chatType;
        this.message  = message;
    }

    public ChatType getChatType() { return chatType; }
    public String getMessage()    { return message; }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("chatType", chatType.name());
        json.addProperty("message", message);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }

}