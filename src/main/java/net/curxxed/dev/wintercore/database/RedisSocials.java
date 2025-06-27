package net.curxxed.dev.wintercore.database;

import redis.clients.jedis.Jedis;

import java.util.Map;
import java.util.UUID;

public class RedisSocials {

    private final Jedis jedis;

    public RedisSocials(String host, int port) {
        this.jedis = new Jedis(host, port);
    }

    public void setSocialLink(UUID uuid, String platform, String value) {
        jedis.hset("social:" + uuid.toString(), platform.toLowerCase(), value);
    }

    public String getSocialLink(UUID uuid, String platform) {
        return jedis.hget("social:" + uuid.toString(), platform.toLowerCase());
    }

    public Map<String, String> getAllSocialLinks(UUID uuid) {
        return jedis.hgetAll("social:" + uuid.toString());
    }
}
