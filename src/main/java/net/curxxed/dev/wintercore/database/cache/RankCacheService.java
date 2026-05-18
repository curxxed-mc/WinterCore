package net.curxxed.dev.wintercore.database.cache;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RankCacheService {

    private static final String DEFAULT_RANK = "Default";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final WinterCore plugin;
    private final ProfileRepository profileRepository;

    private final Map<UUID, String> rankCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRefresh = new ConcurrentHashMap<>();

    public RankCacheService(WinterCore plugin, ProfileRepository profileRepository) {
        this.plugin = plugin;
        this.profileRepository = profileRepository;
    }

    public void get(UUID uuid, Consumer<String> callback) {
        String cached = rankCache.get(uuid);
        if (cached != null) {
            lastRefresh.put(uuid, System.currentTimeMillis());
            if (Bukkit.isPrimaryThread()) {
                callback.accept(cached);
            } else {
                plugin.getTasks().sync(() -> callback.accept(cached));
            }
            return;
        }

        plugin.getTasks().async(() -> {
            String rank;
            try {
                rank = profileRepository.getRank(uuid);
                if (rank == null) rank = DEFAULT_RANK;
            } catch (Exception e) {
                rank = DEFAULT_RANK;
            }

            rankCache.put(uuid, rank);
            lastRefresh.put(uuid, System.currentTimeMillis());
            String finalRank = rank;

            plugin.getTasks().sync(() -> callback.accept(finalRank));
        });
    }

    public String getSync(UUID uuid) {
        String cached = rankCache.get(uuid);
        if (cached != null) {
            lastRefresh.put(uuid, System.currentTimeMillis());
            return cached;
        }
        return DEFAULT_RANK;
    }

    public String peek(UUID uuid) {
        return rankCache.get(uuid);
    }

    public void put(UUID uuid, String rank) {
        rankCache.put(uuid, rank);
        lastRefresh.put(uuid, System.currentTimeMillis());
    }

    public void invalidate(UUID uuid) {
        rankCache.remove(uuid);
        lastRefresh.remove(uuid);
    }

    public void putColor(UUID uuid, String color) {
        colorCache.put(uuid, color);
    }

    public String getColor(UUID uuid) {
        return colorCache.getOrDefault(uuid, "&f");
    }

    public String peekColor(UUID uuid) {
        return colorCache.get(uuid);
    }

    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : lastRefresh.entrySet()) {
            UUID uuid = entry.getKey();

            // Keep cache warm for active players tracked by PlayerService.
            if (plugin.getPlayerService() != null && plugin.getPlayerService().getPlayerData(uuid) != null) {
                lastRefresh.put(uuid, now);
                continue;
            }

            if (now - entry.getValue() <= CACHE_TTL_MS) {
                continue;
            }

            rankCache.remove(uuid);
            colorCache.remove(uuid);
            lastRefresh.remove(uuid);
        }
    }
}
