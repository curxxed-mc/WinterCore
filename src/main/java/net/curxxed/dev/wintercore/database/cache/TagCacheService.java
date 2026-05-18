package net.curxxed.dev.wintercore.database.cache;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TagCacheService {

    private static final String NO_TAG = "__NO_TAG__";

    private final WinterCore plugin;
    private final ProfileRepository profileRepository;

    private final Map<UUID, String> tagCache = new ConcurrentHashMap<>();

    public TagCacheService(WinterCore plugin, ProfileRepository profileRepository) {
        this.plugin = plugin;
        this.profileRepository = profileRepository;
    }

    public void get(UUID uuid, Consumer<String> callback) {
        if (tagCache.containsKey(uuid)) {
            String cached = tagCache.get(uuid);
            plugin.getTasks().sync(() -> callback.accept(NO_TAG.equals(cached) ? null : cached));
            return;
        }

        plugin.getTasks().async(() -> {
            String tag = null;
            try {
                tag = profileRepository.getTag(uuid);
            } catch (Exception ignored) {
            }

            String cachedValue = tag == null ? NO_TAG : tag;
            tagCache.put(uuid, cachedValue);
            String finalTag = NO_TAG.equals(cachedValue) ? null : cachedValue;
            plugin.getTasks().sync(() -> callback.accept(finalTag));
        });
    }

    public String getSync(UUID uuid) {
        String cached = tagCache.get(uuid);
        return NO_TAG.equals(cached) ? null : cached;
    }

    public void put(UUID uuid, String tag) {
        tagCache.put(uuid, tag == null ? NO_TAG : tag);
    }

    public void invalidate(UUID uuid) {
        tagCache.remove(uuid);
    }
}
