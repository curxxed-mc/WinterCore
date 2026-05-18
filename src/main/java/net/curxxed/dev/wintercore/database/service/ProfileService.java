package net.curxxed.dev.wintercore.database.service;

import net.curxxed.dev.wintercore.database.cache.RankCacheService;
import net.curxxed.dev.wintercore.database.cache.TagCacheService;
import net.curxxed.dev.wintercore.database.mongo.ProfileRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ProfileService {

    private final WinterCore plugin;
    private final ProfileRepository profiles;
    private final RankCacheService rankCache;
    private final TagCacheService tagCache;

    public ProfileService(
            WinterCore plugin,
            ProfileRepository profiles,
            RankCacheService rankCache,
            TagCacheService tagCache
    )
    {
        this.plugin = plugin;
        this.profiles = profiles;
        this.rankCache = rankCache;
        this.tagCache = tagCache;
    }

    public void setRank(UUID uuid, String rank) {
        plugin.getTasks().async(() -> {
            try {
                profiles.upsertField(uuid, "rank", rank);
                rankCache.put(uuid, rank);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank for " + uuid, e);
            }
        });
    }

    public void setRankWithMeta(UUID uuid, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        plugin.getTasks().async(() ->
        {
            try {
                Document meta = new Document()
                        .append("grantedBy", grantedBy.toString())
                        .append("grantedAt", grantedAt)
                        .append("expiresAt", expiresAt)
                        .append("reason", reason);

                profiles.upsertField(uuid, "rank", rank);
                profiles.upsertField(uuid, "rank_meta", meta);
                rankCache.put(uuid, rank);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank with meta for " + uuid, e);
            }
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        rankCache.get(uuid, callback);
    }

    public void setPlayerTag(UUID uuid, String tag) {
        plugin.getTasks().async(() ->
        {
            try {
                profiles.upsertField(uuid, "tag", tag);
                tagCache.put(uuid, tag);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not set tag for " + uuid, e);
            }
        });
    }

    public void getPlayerTag(UUID uuid, Consumer<String> callback) {
        tagCache.get(uuid, callback);
    }

    public void setChatColorPreference(UUID uuid, String colorCode) {
        setChatColorPreference(uuid, colorCode, null);
    }

    public void setChatColorPreference(UUID uuid, String colorCode, Runnable callback) {
        plugin.getTasks().async(() ->
        {
            try {
                profiles.upsertField(uuid, "chatColor", colorCode);
                rankCache.putColor(uuid, colorCode);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save chat color for " + uuid, e);
            } finally {
                if (callback != null) {
                    plugin.getTasks().sync( callback);
                }
            }
        });
    }

    public void getChatColorPreference(UUID uuid, Consumer<String> callback) {
        String cached = rankCache.peekColor(uuid);
        if (cached != null) {
            plugin.getTasks().sync(() -> callback.accept(cached));
            return;
        }

        plugin.getTasks().async(() -> {
            String color;
            try {
                color = profiles.getChatColor(uuid);
                if (color == null || color.trim().isEmpty()) {
                    color = "&f";
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch chat color for " + uuid, e);
                color = "&f";
            }

            rankCache.putColor(uuid, color);
            String finalColor = color;
            plugin.getTasks().sync(() -> callback.accept(finalColor));
        });
    }
}
