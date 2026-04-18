package net.curxxed.dev.wintercore.database.cache;

import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TagCacheService {

    private final WinterCore plugin;
    private final ProfileRepository profileRepository;

    private final Map<UUID, String> tagCache = new ConcurrentHashMap<>();

    public TagCacheService(WinterCore plugin, ProfileRepository profileRepository) {
        this.plugin = plugin;
        this.profileRepository = profileRepository;
    }

    public void get(UUID uuid, Consumer<String> callback) {
        if (tagCache.containsKey(uuid)) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tagCache.get(uuid)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String tag = null;
            try {
                tag = profileRepository.getTag(uuid);
            } catch (Exception ignored) {
            }

            tagCache.put(uuid, tag);
            String finalTag = tag;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalTag));
        });
    }

    public String getSync(UUID uuid) {
        return tagCache.get(uuid);
    }

    public void put(UUID uuid, String tag) {
        tagCache.put(uuid, tag);
    }

    public void invalidate(UUID uuid) {
        tagCache.remove(uuid);
    }
}