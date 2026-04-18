package net.curxxed.dev.wintercore.database.redis.packet.packets;

import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.database.redis.handler.RedisPacketHandler;
import net.curxxed.dev.wintercore.database.redis.packet.AbstractRedisPacket;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacketType;

public final class ConfigSyncPacket extends AbstractRedisPacket {

    public enum ConfigType {
        RANKS,
        TAGS
    }

    private final ConfigType configType;
    private final String yaml;

    public ConfigSyncPacket(String sourceServer, long timestamp, ConfigType configType, String yaml) {
        super(RedisPacketType.CONFIG_SYNC, sourceServer, timestamp);
        this.configType = configType;
        this.yaml = yaml;
    }

    public ConfigType getConfigType() {
        return configType;
    }

    public String getYaml() {
        return yaml;
    }

    @Override
    public JsonObject serialize() {
        JsonObject json = baseJson();
        json.addProperty("configType", configType.name());
        json.addProperty("yaml", yaml);
        return json;
    }

    @Override
    public void handle(RedisPacketHandler handler) {
        handler.handle(this);
    }
}