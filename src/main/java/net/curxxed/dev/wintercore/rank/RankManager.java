package net.curxxed.dev.wintercore.rank;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RankManager {

    private static RankManager instance;

    @Getter private final WinterCore plugin;
    @Getter private final RankConfigManager configManager;
    private final RankCacheService cacheService;
    private final RankDisplayManager displayManager;

    private final Map<UUID, String> colorPreferences = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> targetPlayers = new ConcurrentHashMap<>();

    public RankManager(WinterCore plugin) {
        this.plugin = plugin;
        this.configManager = new RankConfigManager(plugin);
        this.cacheService = new RankCacheService(plugin);
        this.displayManager = new RankDisplayManager(plugin, configManager);
    }

    public static void initialize(WinterCore plugin) {
        if (instance == null) instance = new RankManager(plugin);
    }

    public static RankManager getInstance() {
        if (instance == null) throw new IllegalStateException("RankManager has not been initialized yet!");
        return instance;
    }

    public void startAutoCacheRefresh() {
        cacheService.startAutoRefresh();
    }

    public void reloadRanksConfig() {
        configManager.load();
    }

    public void saveRanksConfig() {
        configManager.save();
    }

    public ConfigurationSection getRanksSection() {
        return configManager.getRanksSection();
    }

    public void createRank(String rankName) {
        configManager.createRank(rankName);
    }

    public void deleteRank(String rankName) {
        configManager.deleteRank(rankName);
    }

    public List<String> getAvailableRanks() {
        return configManager.getAvailableRanks();
    }

    public List<String> getSortedRanks() {
        return configManager.getSortedRanks();
    }

    public List<String> getPermissionsForRank(String rank) {
        return configManager.getPermissionsForRank(rank);
    }

    public List<String> getLowerRanks(String playerRank) {
        return Rank.getLowerRanks(playerRank, plugin);
    }

    public void setRank(Player player, String rank, Player giver) {
        plugin.getDatabaseManager().setRank(player.getUniqueId(), rank);
        cacheService.put(player.getUniqueId(), rank);

        String color = configManager.getColor(rank);
        cacheService.putColor(player.getUniqueId(), color);

        displayManager.sendRankUpdateToBungee(player.getName(), rank);
        Bukkit.getPluginManager().callEvent(new RankChangeEvent(player, rank));
        displayManager.applyRank(player, rank, color);

        player.sendMessage(CC.translate("&aYour rank has been set to " + rank + " by " + giver.getName() + "."));
        giver.sendMessage(CC.translate("&aYou have set " + player.getName() + "'s rank to " + rank + "."));
    }

    public void getRank(Player player, Consumer<String> callback) {
        plugin.getRedisManager().getDisguise(player.getUniqueId(), disguiseJson -> {
            String disguiseRank = extractDisguiseField(disguiseJson, "rank");
            if (disguiseRank != null) { callback.accept(disguiseRank); return; }
            cacheService.get(player.getUniqueId(), callback);
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        cacheService.get(uuid, callback);
    }

    public String getRankSync(Player player) {
        return cacheService.getSync(player.getUniqueId());
    }

    public String getRankSync(UUID uuid) {
        return cacheService.getSync(uuid);
    }

    public void cachePlayerRank(Player player, String rank) {
        cacheService.put(player.getUniqueId(), rank);
    }

    public void refreshCache(Player player) {
        cacheService.invalidate(player.getUniqueId());
        cacheService.get(player.getUniqueId(), rank -> refreshPlayerDisplay(player));
    }

    public void getColorPreference(String rank, Consumer<String> callback) {
        callback.accept(configManager.getColor(rank));
    }

    public String getColorPreferenceSync(Player player) {
        String cached = cacheService.getColor(player.getUniqueId());
        if (!"&f".equals(cached)) return cached;
        return configManager.getColor(cacheService.getSync(player.getUniqueId()));
    }

    public void cachePlayerColor(Player player, String color) {
        cacheService.putColor(player.getUniqueId(), color);
    }

    public String getCachedColor(UUID uuid) {
        return cacheService.getColor(uuid);
    }

    public void getRankPrefix(Player player, Consumer<String> callback) {
        getRank(player, rank -> callback.accept(configManager.getPrefix(rank)));
    }

    public String getRankPrefixSync(Player player) {
        return configManager.getPrefix(getRankSync(player));
    }

    public void refreshPlayerDisplay(Player player) {
        getRank(player, rank -> {
            String color = configManager.getColor(rank);
            cacheService.putColor(player.getUniqueId(), color);
            displayManager.refreshDisplay(player, rank, color);
        });
    }

    public void refreshPlayerDisplayForAll(Player target) {
        getRank(target, rank -> {
            String color = configManager.getColor(rank);
            cacheService.putColor(target.getUniqueId(), color);
            displayManager.refreshDisplayForAll(target, rank, color);
        });
    }

    public void setRankAboveHead(Player player) {
        getRank(player, rank -> displayManager.applyNameTag(player, configManager.getColor(rank)));
    }

    public void updateNameTagColor(Player player, String color) {
        displayManager.applyNameTag(player, color != null ? color : "&f");
    }

    public void getDisguiseRank(Player player, Consumer<String> callback) {
        plugin.getRedisManager().getDisguise(player.getUniqueId(), disguiseJson -> {
            String disguiseRank = extractDisguiseField(disguiseJson, "rank");
            if (disguiseRank != null) { callback.accept(disguiseRank); return; }
            getRank(player, callback);
        });
    }

    public String getDisguiseRankSync(Player player) {
        String color = extractDisguiseField(plugin.getRedisManager().getDisguiseSync(player.getUniqueId()), "rank");
        return color != null ? color : getRankSync(player);
    }

    public String getDisguiseColorPreferenceSync(Player player) {
        String color = extractDisguiseField(plugin.getRedisManager().getDisguiseSync(player.getUniqueId()), "color");
        return color != null ? color : getColorPreferenceSync(player);
    }

    public void setMessageColorPreference(Player player, String colorCode) {
        colorPreferences.put(player.getUniqueId(), colorCode);
    }

    public String getMessageColorPreference(Player player) {
        return colorPreferences.getOrDefault(player.getUniqueId(), "&f");
    }

    public void setTargetPlayer(Player player, Player target) {
        targetPlayers.put(player.getUniqueId(), target.getUniqueId());
    }

    public Player getTargetPlayer(Player player) {
        UUID uuid = targetPlayers.get(player.getUniqueId());
        return uuid != null ? Bukkit.getPlayer(uuid) : null;
    }

    public void setTargetPlayerUUID(UUID staffUUID, UUID targetUUID) {
        targetPlayers.put(staffUUID, targetUUID);
    }

    public UUID getTargetPlayerUUID(UUID staffUUID) {
        return targetPlayers.get(staffUUID);
    }

    public void removeTargetPlayerUUID(UUID staffUUID) {
        targetPlayers.remove(staffUUID);
    }

    private String extractDisguiseField(String disguiseJson, String field) {
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