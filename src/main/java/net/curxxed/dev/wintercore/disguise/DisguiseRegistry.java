package net.curxxed.dev.wintercore.disguise;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.curxxed.dev.wintercore.database.RedisManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class DisguiseRegistry {

    private final Map<UUID, SkinFetcher.SkinProperty> originalSkins = new ConcurrentHashMap<>();
    public final Set<UUID> disguisedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> colorCache = new ConcurrentHashMap<>();

    private final RedisManager redisManager;
    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DisguiseRegistry(RedisManager redisManager, Logger logger) {
        this.redisManager = redisManager;
        this.logger = logger;
    }

    public boolean isDisguised(Player player) {
        return disguisedPlayers.contains(player.getUniqueId());
    }

    public SkinFetcher.SkinProperty getOriginalSkin(Player player) {
        return originalSkins.get(player.getUniqueId());
    }

    public void setDisguised(Player player, SkinFetcher.SkinProperty skin) {
        originalSkins.put(player.getUniqueId(), skin);
        disguisedPlayers.add(player.getUniqueId());
    }

    public void clear(Player player) {
        clear(player.getUniqueId());
    }

    public void clear(UUID uuid) {
        originalSkins.remove(uuid);
        disguisedPlayers.remove(uuid);
    }

    public void clearAll(Consumer<Player> undisguiseAction) {
        for (UUID uuid : new HashSet<>(disguisedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                try {
                    undisguiseAction.accept(player);
                } catch (Exception ignored) {}
            }
        }
    }

    public void handleQuit(Player player, Consumer<Player> undisguiseAction) {
        UUID uuid = player.getUniqueId();
        scheduler.schedule(() -> {
            if (!redisManager.isStillPendingSwitch(uuid) && isDisguised(player)) {
                try {
                    undisguiseAction.accept(player);
                } catch (Exception e) {
                    logger.warning("Failed to undisguise player on quit: " + player.getName());
                }
                clear(uuid);
            }
        }, 4, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void setDisguiseInfo(Player player, String disguiseName, String disguiseRank, String skin, String color, String prefix) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", disguiseName);
        obj.addProperty("rank", disguiseRank);
        obj.addProperty("skin", skin);
        obj.addProperty("color", color);
        obj.addProperty("prefix", prefix);
        redisManager.setDisguise(player.getUniqueId(), obj.toString());
    }

    public void clearDisguiseInfo(Player player) {
        redisManager.clearDisguise(player.getUniqueId());
    }

    public void getEffectiveRank(Player player, Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            String rank = extractField(disguiseJson, "rank");
            if (rank != null) { callback.accept(rank); return; }
            RankManager.getInstance().getRank(player, callback);
        });
    }

    public void getEffectiveColor(Player player, Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            String color = extractField(disguiseJson, "color");
            if (color != null) { callback.accept(color); return; }
            RankManager rankManager = RankManager.getInstance();
            rankManager.getRank(player, rank -> rankManager.getColorPreference(rank, callback));
        });
    }

    public String getEffectiveColorSync(Player player) {
        String color = extractField(redisManager.getDisguiseSync(player.getUniqueId()), "color");
        return color != null ? color : RankManager.getInstance().getColorPreferenceSync(player);
    }

    public void getEffectivePrefix(Player player, Consumer<String> callback) {
        redisManager.getDisguise(player.getUniqueId(), disguiseJson -> {
            String prefix = extractField(disguiseJson, "prefix");
            if (prefix != null) { callback.accept(prefix); return; }
            RankManager.getInstance().getRankPrefix(player, callback);
        });
    }

    public void updateColorCache(Player player) {
        UUID uuid = player.getUniqueId();
        redisManager.getDisguise(uuid, disguiseJson -> {
            String color = extractField(disguiseJson, "color");
            if (color == null) color = RankManager.getInstance().getColorPreferenceSync(player);
            colorCache.put(uuid, color);
            Bukkit.getScheduler().runTask(WinterCore.INSTANCE,
                    () -> WinterCore.INSTANCE.getNameTagHandler().updateNameTagFor(player));
        });
    }

    public String getCachedColor(UUID uuid) {
        return colorCache.getOrDefault(uuid, "&f");
    }

    public void removeColorCache(UUID uuid) {
        colorCache.remove(uuid);
    }

    private String extractField(String disguiseJson, String field) {
        if (disguiseJson == null) return null;
        try {
            JsonObject obj = new JsonParser().parse(disguiseJson).getAsJsonObject();
            if (obj.has(field) && !obj.get(field).isJsonNull() && !obj.get(field).getAsString().isEmpty()) {
                return obj.get(field).getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}