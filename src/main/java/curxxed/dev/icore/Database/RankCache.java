package curxxed.dev.icore.Database;

import java.util.HashMap;
import java.util.UUID;

public class RankCache {

    private final HashMap<UUID, String> cache = new HashMap<>();

    public void set(UUID uuid, String rank) {
        cache.put(uuid, rank);
    }

    public String get(UUID uuid) {
        return cache.getOrDefault(uuid, "Default");
    }

    public boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }
}
