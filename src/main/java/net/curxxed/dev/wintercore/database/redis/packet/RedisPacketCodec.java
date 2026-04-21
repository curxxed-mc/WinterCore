package net.curxxed.dev.wintercore.database.redis.packet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.packet.packets.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RedisPacketCodec {

    private final Gson gson;
    private final Map<RedisPacketType, PacketDeserializer> deserializers = new HashMap<>();

    public RedisPacketCodec(Gson gson) {
        this.gson = gson;
        registerDefaults();
    }

    private void registerDefaults() {
        deserializers.put(RedisPacketType.RANK_TAG_SYNC, json -> new RankTagSyncPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                UUID.fromString(json.get("targetUuid").getAsString()),
                json.has("rank") && !json.get("rank").isJsonNull()
                        ? json.get("rank").getAsString()
                        : ""
        ));

        deserializers.put(RedisPacketType.SERVER_SWITCH, json -> new ServerSwitchPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                UUID.fromString(json.get("uuid").getAsString()),
                json.get("previousServer").getAsString(),
                json.get("currentServer").getAsString()
        ));

        deserializers.put(RedisPacketType.SERVER_STATUS, json -> new ServerStatusPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                json.get("online").getAsBoolean()
        ));

        deserializers.put(RedisPacketType.STAFF_ACTIVITY, json -> new StaffActivityPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                json.get("activityType").getAsString(),
                json.get("playerName").getAsString(),
                json.get("color").getAsString(),
                json.get("fromServer").getAsString(),
                json.get("toServer").getAsString()
        ));

        deserializers.put(RedisPacketType.REMOTE_COMMAND, json -> new RemoteCommandPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                json.get("targetServer").getAsString(),
                json.get("command").getAsString()
        ));

        deserializers.put(RedisPacketType.PLAYER_REPORT, json -> new PlayerReportPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                json.get("reporter").getAsString(),
                json.get("reported").getAsString(),
                json.get("reason").getAsString(),
                json.get("server").getAsString()
        ));

        deserializers.put(RedisPacketType.DISGUISE_STATE, json -> new DisguiseStatePacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                UUID.fromString(json.get("uuid").getAsString()),
                json.get("disguised").getAsBoolean(),
                json.has("disguiseJson") && !json.get("disguiseJson").isJsonNull()
                        ? json.get("disguiseJson").getAsString()
                        : null
        ));

        deserializers.put(RedisPacketType.CONFIG_SYNC, json -> new ConfigSyncPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                ConfigSyncPacket.ConfigType.valueOf(json.get("configType").getAsString()),
                json.get("yaml").getAsString()
        ));
        deserializers.put(RedisPacketType.CHAT_BROADCAST, json -> new ChatBroadcastPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                ChatBroadcastPacket.ChatType.valueOf(json.get("chatType").getAsString()),
                json.get("message").getAsString()
        ));
        deserializers.put(RedisPacketType.VANISH_STATE, json -> new VanishPacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                UUID.fromString(json.get("playerUuid").getAsString()),
                json.get("playerName").getAsString(),
                json.get("vanished").getAsBoolean()
        ));
        deserializers.put(RedisPacketType.PLAYER_UPDATE, json -> new PlayerUpdatePacket(
                json.get("sourceServer").getAsString(),
                json.get("timestamp").getAsLong(),
                UUID.fromString(json.get("targetUuid").getAsString())
        ));
    }

    public String encode(RedisPacket<?> packet) {
        return gson.toJson(packet.serialize());
    }

    public RedisPacket<?> decode(String raw) {
        JsonObject json = gson.fromJson(raw, JsonObject.class);
        RedisPacketType type = RedisPacketType.valueOf(json.get("type").getAsString());

        PacketDeserializer deserializer = deserializers.get(type);
        if (deserializer == null) {
            throw new IllegalArgumentException("No deserializer registered for packet type " + type);
        }

        return deserializer.deserialize(json);
    }

    private interface PacketDeserializer {
        RedisPacket<?> deserialize(JsonObject json);
    }
}
