package net.curxxed.dev.icore.rank;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.nametags.NameTag;
import net.curxxed.dev.icore.nametags.NameTagAdapter;
import net.curxxed.dev.icore.permissions.iCorePermissible;
import net.curxxed.dev.icore.permissions.iCorePermissibleInjector;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.CC;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class RankManager {
    @Getter
    private final iCore plugin;
    private final Map<UUID, String> playerRanks = new HashMap<>();
    private final Map<UUID, String> colorPreferences = new HashMap<>();
    private final Map<UUID, String> playerCache = new HashMap<>();
    private final Map<UUID, String> colorCache = new HashMap<>();
    private final Map<UUID, Player> targetPlayers = new HashMap<>();
    private final NameTagAdapter nameTagAdapter;
    private final DatabaseManager databaseManager;
    private FileConfiguration ranksConfig;

    private static final String DEFAULT_RANK = "Default";
    private static RankManager instance;
    private final Map<UUID, Long> lastRefresh = new HashMap<>();


    public RankManager(iCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.nameTagAdapter = new NameTag();
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(ranksFile.toPath()), StandardCharsets.UTF_8)) {
            this.ranksConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load ranks.yml with UTF-8: " + e.getMessage());
            this.ranksConfig = YamlConfiguration.loadConfiguration(ranksFile); // fallback
        }
    }

    public void reloadRanksConfig() {
        File ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(ranksFile.toPath()), StandardCharsets.UTF_8)) {
            this.ranksConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to reload ranks.yml with UTF-8: " + e.getMessage());
            this.ranksConfig = YamlConfiguration.loadConfiguration(ranksFile); // fallback
        }
    }

    public void saveRanksConfig() {
        try {
            ranksConfig.save(new File(plugin.getDataFolder(), "ranks.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save ranks.yml: " + e.getMessage());
        }
    }


    public static RankManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RankManager has not been initialized yet!");
        }
        return instance;
    }

    public ConfigurationSection getRanksSection() {
        return ranksConfig.getConfigurationSection("ranks");
    }

    public static void initialize(iCore plugin) {
        if (instance == null) {
            instance = new RankManager(plugin);
        }
    }



    public void setRank(Player player, String rank, Player giver) {
        try {
            databaseManager.setRank(player.getUniqueId(), rank);
            playerRanks.put(player.getUniqueId(), rank);
            playerCache.put(player.getUniqueId(), rank);
            sendRankUpdateToBungee(player.getName(), rank);

            // Fire the RankChangeEvent
            RankChangeEvent e = new RankChangeEvent(player, rank);
            Bukkit.getPluginManager().callEvent(e);

            // Fetch rank and color synchronously before updating visuals
            getRank(player, updatedRank -> getColorPreference(updatedRank, rankColor -> {
                cachePlayerColor(player, rankColor); // Cache the color
                refreshPlayerRankAndNameTag(player); // Update display name and permissions
                setRankAboveHead(player); // Update the name tag above the player's head

                // Notify players
                player.sendMessage(CC.translate("&aYour rank has been set to " + rank + " by " + giver.getName() + "."));
                giver.sendMessage(CC.translate("&aYou have successfully set the rank of " + player.getName() + " to " + rank + "."));
            }));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to set rank for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshPlayerRankAndNameTag(Player player) {
        getRank(player, rank -> getColorPreference(rank, rankColor -> {
            updatePlayerRank(player); // Update display name and permissions
            // Convert color code (e.g., "&b") to Bukkit ChatColor
            ChatColor chatColor = ChatColor.getByChar(rankColor.replace("&", "").charAt(0));
            nameTagAdapter.setNameTag(player, "", chatColor);
        }));
    }

    private void invalidatePlayerCache(Player player) {
        playerRanks.remove(player.getUniqueId());
        getRankAsync(player, rank -> refreshPlayerDisplay(player));
    }

    public void createRank(String rankName) {
        ConfigurationSection ranks = getRanksSection();
        if (ranks == null) {
            ranks = ranksConfig.createSection("ranks");
        }

        if (ranks.contains(rankName)) {
            throw new IllegalArgumentException("rank already exists!");
        }

        ConfigurationSection newRank = ranks.createSection(rankName);
        newRank.set("prefix", "");
        newRank.set("weight", 0);
        newRank.set("name-color", "&f");

        saveRanksConfig();
    }

    public void deleteRank(String rankName) {
        if (!getRanksSection().contains(rankName)) {
            throw new IllegalArgumentException("rank does not exist!");
        }
        getRanksSection().set(rankName, null);
        saveRanksConfig();
    }

    public void closeDatabaseConnection() {
        databaseManager.close();
    }

    public void sendRankUpdateToBungee(String playerName, String rank) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("SyncRank");

        out.writeUTF(playerName);
        out.writeUTF(rank);

        Player player = Bukkit.getOnlinePlayers().iterator().next();
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }



    public void getRank(Player player, Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.getRank(player.getUniqueId(), callback));
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.getRank(uuid, rank -> {
            playerRanks.put(uuid, rank);
            callback.accept(rank);
        }));
    }



    public void getRankAsync(Player player, Consumer<String> callback) {
        databaseManager.getRank(player.getUniqueId(), rank -> {
            playerCache.put(player.getUniqueId(), rank);
            callback.accept(rank);
        });
    }

    public void getRankAsync(UUID uuid, Consumer<String> callback) {
        databaseManager.getRank(uuid, rank -> {
            playerCache.put(uuid, rank);
            callback.accept(rank);
        });
    }



    public void cachePlayerRank(Player player, String rank) {
        UUID uuid = player.getUniqueId();
        playerCache.put(uuid, rank);
        playerRanks.put(uuid, rank);
    }




    public void refreshCache(Player player) {
        invalidatePlayerCache(player);
        getRankSync(player);
    }


    public String getRankSync(Player player) {
        if (!playerRanks.containsKey(player.getUniqueId())) {
            CompletableFuture<String> future = new CompletableFuture<>();

            getRankAsync(player, rank -> {
                playerRanks.put(player.getUniqueId(), rank);
                future.complete(rank);
            });

            try {
                return future.get(1, TimeUnit.NANOSECONDS);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get rank for player " + player.getName() + ": ");
                return DEFAULT_RANK;
            }
        }
        return playerRanks.getOrDefault(player.getUniqueId(), DEFAULT_RANK);
    }

    public String getRankSync(UUID uuid) {
        if (playerRanks.containsKey(uuid)) {
            return playerRanks.get(uuid);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        getRank(uuid, future::complete);

        try {
            return future.get(1, TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get rank for UUID " + uuid + ": " + e.getMessage());
            return DEFAULT_RANK;
        }

    }


    public void saveNewRank(String rankName, String rankPrefix) {
        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
        if (ranks == null) {
            ranks = ranksConfig.createSection("ranks");
        }

        int nextWeight = getNextAvailableWeight(ranks);
        ConfigurationSection newRank = ranks.createSection(rankName);
        newRank.set("prefix", rankPrefix != null ? rankPrefix : "");
        newRank.set("weight", getNextAvailableWeight(ranks));
        newRank.set("name-color", "&f");

        plugin.saveConfig();
    }

    private int getNextAvailableWeight(ConfigurationSection ranks) {
        int highestWeight = 0;

        for (String rank : ranks.getKeys(false)) {
            int weight = ranks.getConfigurationSection(rank).getInt("weight", 0);
            if (weight > highestWeight) {
                highestWeight = weight;
            }
        }

        return highestWeight + 1;
    }


    public List<String> getSortedRanks() {
        List<String> sortedRanks = new ArrayList<>();
        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");

        if (ranks != null) {
            List<Map.Entry<String, Integer>> rankEntries = new ArrayList<>();

            for (String rank : ranks.getKeys(false)) {
                int weight = ranks.getConfigurationSection(rank).getInt("weight", 0);
                rankEntries.add(new AbstractMap.SimpleEntry<>(rank, weight));
            }
            rankEntries.sort((entry1, entry2) -> {
                int weightComparison = Integer.compare(entry2.getValue(), entry1.getValue());
                if (weightComparison != 0) {
                    return weightComparison;
                }
                return entry1.getKey().compareToIgnoreCase(entry2.getKey());
            });

            for (Map.Entry<String, Integer> entry : rankEntries) {
                sortedRanks.add(entry.getKey());
            }
        }

        return sortedRanks;
    }


    // src/main/java/curxxed/dev/icore/rank/RankManager.java
    private void updatePlayerRank(Player player) {
        getRankAsync(player, rank -> {
            if (rank == null) {
                player.setDisplayName(CC.translate("&7" + player.getName()));
                player.setPlayerListName(CC.translate("&7" + player.getName()));
                return;
            }
            try {
                iCorePermissible permissible = (iCorePermissible) iCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
                permissible.clearRawPermissions(); // Clear only raw permissions
                List<String> permissions = getPermissionsForRank(rank);
                for (String permission : permissions) {
                    permissible.addRawPermission(permission, true); // Use the method, not direct map put
                }
                permissible.recalculatePermissions(); // Recalculate after all are added
            } catch (IllegalAccessException e) {
                plugin.getLogger().severe("Failed to update permissions for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
            String nameColor = ranksConfig.getString("ranks." + rank + ".name-color", "&f");
            String coloredName = CC.translate(nameColor) + player.getName() + org.bukkit.ChatColor.RESET;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setDisplayName(coloredName);
                player.setPlayerListName(coloredName);
                player.setCustomName(coloredName);
                player.setCustomNameVisible(true);
            });
        });
    }

    private void grantPermissionsAsync(Player player, String rank) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> permissions = getPermissionsForRank(rank);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String permission : permissions) {
                    if (!player.hasPermission(permission)) {
                        player.addAttachment(plugin, permission, true);
                    }
                }
            });
        });
    }


    public List<String> getPermissionsForRank(String rank) {
        List<String> permissions = new ArrayList<>();
        List<?> rawList = ranksConfig.getList("ranks." + rank + ".permissions", new ArrayList<>());
        for (Object obj : rawList) {
            if (obj instanceof String) {
                permissions.add((String) obj);
            } else if (obj instanceof Map) {
                Object perm = ((Map<?, ?>) obj).get("permission");
                if (perm instanceof String) {
                    permissions.add((String) perm);
                }
            }
        }
        // Handle inheritance as before
        List<String> parents = ranksConfig.getStringList("ranks." + rank + ".inheritance");
        for (String parent : parents) {
            permissions.addAll(getPermissionsForRank(parent));
        }
        return permissions;
    }



    private void addInheritedPermissions(String rank, ConfigurationSection ranksSection, Set<String> permissions) {
        Rank rankObj = new Rank(rank, plugin);

        // Extract permissions from the Map<String, String>
        for (Map<String, String> permissionMap : rankObj.getPermissions()) {
            if (permissionMap.containsKey("permission")) {
                permissions.add(permissionMap.get("permission"));
            }
        }

        // Add inherited permissions
        for (String inheritedRank : rankObj.getInheritance()) {
            addInheritedPermissions(inheritedRank, ranksSection, permissions);
        }
    }

    public void getRankPrefix(Player player, Consumer<String> callback) {
        getRankAsync(player, rank -> {
            ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
            if (ranks == null) {
                plugin.getLogger().warning("Ranks section is missing in ranks.yml");
                callback.accept("");
                return;
            }

            ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
            if (rankSection != null) {
                String prefix = rankSection.getString("prefix", "");
                String coloredPrefix = CC.translate(prefix);
                callback.accept(coloredPrefix);
            } else {
                callback.accept("");
            }
        });
    }


    public String getColorPreferenceSync(Player player) {
        String rank = playerCache.getOrDefault(player.getUniqueId(), "Default");
        if (rank == null) return "&f";

        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
        if (ranks == null) {
            plugin.getLogger().warning("Ranks section is missing in ranks.yml");
            return "&f";
        }

        return ranks.getString(rank + ".name-color", "&f");
    }

    public String getRankPrefixSync(Player player) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        getRankPrefix(player, future::complete);
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }





    public List<String> getAvailableRanks() {
        List<String> availableRanks = new ArrayList<>();
        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
        if (ranks != null) {
            availableRanks.addAll(ranks.getKeys(false));
        }
        return availableRanks;
    }

    public void getColorPreference(String rank, Consumer<String> callback) {
        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
        if (ranks != null && ranks.contains(rank + ".name-color")) {
            String color = ranks.getString(rank + ".name-color", "&f");
            callback.accept(color);
        } else {
            callback.accept("&f");
        }
    }

    public void startAutoCacheRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!lastRefresh.containsKey(player.getUniqueId()) || now - lastRefresh.get(player.getUniqueId()) > 1000) {
                    getRankAsync(player, fetchedRank -> {
                        playerRanks.put(player.getUniqueId(), fetchedRank);
                        lastRefresh.put(player.getUniqueId(), now);
                    });
                }
            }
        }, 0L, 20L);
    }

    public void refreshPlayerDisplay(Player player) {
        getRank(player, rank -> getColorPreference(rank, rankColor -> {
            String formattedName = CC.translate(rankColor) + player.getName();

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setDisplayName(formattedName); // Display name in chat
                player.setPlayerListName(formattedName); // Name in tab list
                player.setCustomName(formattedName); // Name above the player's head
                player.setCustomNameVisible(true);
                updatePlayerRank(player);
            });
        }));
    }

    private void fetchColorFromRank(String rank, Consumer<String> callback) {
        ConfigurationSection ranks = ranksConfig.getConfigurationSection("ranks");
        if (ranks != null) {
            ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
            if (rankSection != null) {
                String color = rankSection.getString("name-color", "&f"); // Default color
                callback.accept(color);
                return;
            }
        }
        callback.accept("&f");
    }




    public void setMessageColorPreference(Player player, String colorCode) {
        colorPreferences.put(player.getUniqueId(), colorCode);
    }

    // Getter
    public CC getMessageColorPreference(Player player) {
        String colorCode = colorPreferences.getOrDefault(player.getUniqueId(), "&f");
        return new CC() {
            @Override
            public String toString() {
                return colorCode;
            }
        };
    }

    public String getCachedColor(UUID uuid) {
        return colorCache.getOrDefault(uuid, "&f");
    }

    public void cachePlayerColor(Player player, String color) {
        colorCache.put(player.getUniqueId(), color);
    }



    public void setTargetPlayer(Player player, Player targetPlayer) {
        targetPlayers.put(player.getUniqueId(), targetPlayer);
    }


    public Player getTargetPlayer(Player player) {
        return targetPlayers.get(player.getUniqueId());
    }

    public List<String> getLowerRanks(String playerRank) {
        return Rank.getLowerRanks(playerRank, plugin); // Delegate to rank class
    }

    public void setRankAboveHead(Player player) {
        boolean showRank = ranksConfig.getBoolean("show-rank-above-head", true);

        if (showRank) {
            getRank(player, rank -> getColorPreference(rank, rankColor -> getRankPrefix(player, prefix -> {
                try {
                    ChatColor chatColor = ChatColor.getByChar(rankColor.replace("&", "").charAt(0));
                    nameTagAdapter.setNameTag(player, prefix, chatColor);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to set name tag color for player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            })));
        } else {
            nameTagAdapter.resetNameTag(player);
        }
    }

    public void updateNameTagColor(Player player, org.bukkit.ChatColor color) {
        getRankPrefix(player, prefix -> {
            String formattedPrefix = CC.translate(prefix);
            nameTagAdapter.setNameTag(player, formattedPrefix, color);
        });
    }
}
