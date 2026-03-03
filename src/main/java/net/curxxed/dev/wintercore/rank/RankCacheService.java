package net.curxxed.dev.wintercore.rank;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RankCacheService {

    private static final String DEFAULT_RANK = "Default";
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final WinterCore plugin;
    private final DatabaseManager databaseManager;

    private final Map<UUID, String> rankCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRefresh = new ConcurrentHashMap<>();

    public RankCacheService(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (now - lastRefresh.getOrDefault(uuid, 0L) > CACHE_TTL_MS) {
                    databaseManager.getRank(uuid, rank -> {
                        rankCache.put(uuid, rank);
                        lastRefresh.put(uuid, now);
                    });
                }
            }
        }, 0L, 6000L);
    }

    public void get(UUID uuid, Consumer<String> callback) {
        String cached = rankCache.get(uuid);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                databaseManager.getRank(uuid, rank -> {
                    rankCache.put(uuid, rank);
                    lastRefresh.put(uuid, System.currentTimeMillis());
                    callback.accept(rank);
                })
        );
    }

    public String getSync(UUID uuid) {
        return rankCache.getOrDefault(uuid, DEFAULT_RANK);
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
}