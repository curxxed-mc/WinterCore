package curxxed.dev.icore.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftScoreboard;
import org.bukkit.entity.Player;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class RankManager {
    @Getter
    private final Main plugin;
    private final Map<UUID, String> playerRanks = new HashMap<>();
    private final Map<UUID, ChatColor> colorPreferences = new HashMap<>();
    private final Map<UUID, String> playerCache = new HashMap<>();
    private final Map<UUID, String> colorCache = new HashMap<>();
    private final Map<UUID, Player> targetPlayers = new HashMap<>();
    private final DatabaseManager databaseManager;

    private static final String DEFAULT_RANK = "Default";
    private static RankManager instance;
    private final Map<UUID, Long> lastRefresh = new HashMap<>();


    public RankManager(Main plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }


    public static RankManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RankManager has not been initialized yet!");
        }
        return instance;
    }

    public static void initialize(Main plugin) {
        if (instance == null) {
            instance = new RankManager(plugin);
        }
    }



    public void setRank(Player player, String rank, Player giver) {
        databaseManager.setRank(player.getUniqueId(), rank);
        sendRankUpdateToBungee(player.getName(), rank);
        invalidatePlayerCache(player); // Invalidate cache
        getRankAsync(player, fetchedRank -> plugin.getLogger().info("Cache refreshed automatically after rank set for " + player.getName()));

        // Refresh display name with the new rank's prefix and color
        refreshPlayerDisplay(player);

        plugin.getLogger().info("Set rank of " + player.getName() + " to " + rank);
        player.sendMessage(ChatColor.GREEN + "Your rank has been set to " + rank + " by " + giver.getName() + ".");
        giver.sendMessage(ChatColor.GREEN + "You have successfully set the rank of " + player.getName() + " to " + rank + ".");
        updatePlayerRank(player); // Update display name + permissions
        setRankAboveHead(player); // Update scoreboard color above head
    }



    private void invalidatePlayerCache(Player player) {
        playerRanks.remove(player.getUniqueId());
        getRankAsync(player, rank -> refreshPlayerDisplay(player));
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

        // Sending through an online player (required by Bukkit)
        Player player = Bukkit.getOnlinePlayers().iterator().next();
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }



    public void getRank(Player player, Consumer<String> callback) {
        // Ensure the database call happens asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.getRank(player.getUniqueId(), callback);
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.getRank(uuid, rank -> {
                playerRanks.put(uuid, rank); // Cache it
                callback.accept(rank);
            });
        });
    }



    public void getRankAsync(Player player, Consumer<String> callback) {
        databaseManager.getRank(player.getUniqueId(), rank -> {
            playerCache.put(player.getUniqueId(), rank); // Cache the rank
            callback.accept(rank);
        });
    }

    public void getRankAsync(UUID uuid, Consumer<String> callback) {
        databaseManager.getRank(uuid, rank -> {
            playerCache.put(uuid, rank); // Cache it
            callback.accept(rank);
        });
    }



    public void cachePlayerRank(Player player, String rank) {
        playerCache.put(player.getUniqueId(), rank);
    }



    public void refreshCache(Player player) {
        invalidatePlayerCache(player);
        getRankSync(player);
    }


    public String getRankSync(Player player) {
        if (!playerRanks.containsKey(player.getUniqueId())) {
            CompletableFuture<String> future = new CompletableFuture<>();

            getRankAsync(player, rank -> {
                playerRanks.put(player.getUniqueId(), rank); // Store in cache
                future.complete(rank); // Complete the future
            });

            try {
                return future.get(2, TimeUnit.SECONDS); // Wait max 2 seconds
            } catch (Exception e) {
                e.printStackTrace();
                return DEFAULT_RANK; // Timeout fallback
            }
        }
        return playerRanks.getOrDefault(player.getUniqueId(), DEFAULT_RANK); // Use cache if already exists
    }

    public String getRankSync(UUID uuid) {
        if (playerRanks.containsKey(uuid)) {
            return playerRanks.get(uuid);
        }

        CompletableFuture<String> future = new CompletableFuture<>();

        getRank(uuid, future::complete);

        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get rank for UUID " + uuid + ": " + e.getMessage());
            return DEFAULT_RANK;
        }
    }


    public void saveNewRank(String rankName, String rankPrefix) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            ranks = plugin.getConfig().createSection("ranks");
        }

        int nextWeight = getNextAvailableWeight(ranks);
        ConfigurationSection newRank = ranks.createSection(rankName);
        newRank.set("prefix", rankPrefix);
        newRank.set("weight", nextWeight);
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
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");

        if (ranks != null) {
            List<Map.Entry<String, Integer>> rankEntries = new ArrayList<>();

            for (String rank : ranks.getKeys(false)) {
                int weight = ranks.getConfigurationSection(rank).getInt("weight", 0);
                rankEntries.add(new AbstractMap.SimpleEntry<>(rank, weight));
            }

            // Sort by weight (descending) and then by rank name (ascending)
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


    public void updatePlayerRank(Player player) {
        getRankAsync(player, rank -> {
            ConfigurationSection rankSection = plugin.getConfig().getConfigurationSection("ranks").getConfigurationSection(rank);

            if (rankSection == null) {
                player.setDisplayName(ChatColor.GRAY + player.getName());
                player.setPlayerListName(ChatColor.GRAY + player.getName());
                return;
            }

            String nameColor = rankSection.getString("name-color", "&f");
            String coloredName = ChatColor.translateAlternateColorCodes('&', nameColor) + player.getName() + ChatColor.RESET;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setDisplayName(coloredName);
                player.setPlayerListName(coloredName);
                player.setCustomName(coloredName);
                player.setCustomNameVisible(true);
            });

            grantPermissionsAsync(player, rank);
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
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return new ArrayList<>();
        }

        Set<String> permissions = new HashSet<>();
        ConfigurationSection rankSection = ranksSection.getConfigurationSection(rank);
        if (rankSection != null && rankSection.contains("permission")) {
            permissions.add(rankSection.getString("permission"));
        }

        addInheritedPermissions(rank, ranksSection, permissions);

        return new ArrayList<>(permissions);
    }

    private void addInheritedPermissions(String rank, ConfigurationSection ranksSection, Set<String> permissions) {
        ConfigurationSection rankSection = ranksSection.getConfigurationSection(rank);
        if (rankSection == null || !rankSection.contains("inherits")) {
            return;
        }

        List<String> inherits = rankSection.getStringList("inherits");
        for (String inheritedRank : inherits) {
            ConfigurationSection inheritedRankSection = ranksSection.getConfigurationSection(inheritedRank);
            if (inheritedRankSection != null && inheritedRankSection.contains("permission")) {
                permissions.add(inheritedRankSection.getString("permission"));
            }
            addInheritedPermissions(inheritedRank, ranksSection, permissions);
        }
    }

    public String fetchRankPrefix(Player player) {
        // Get the player's rank synchronously from the cache (or database)
        String rank = getRankSync(player); // This will use the getRankSync method

        // Fetch the configuration section
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return "";  // Return empty string if ranks are missing
        }

        // Get the configuration section for the player's rank
        ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
        if (rankSection != null) {
            String prefix = rankSection.getString("prefix", "");
            return ChatColor.translateAlternateColorCodes('&', prefix); // Return the translated color code
        } else {
            return "";  // Default empty string if prefix is not set
        }
    }



    public void getRankPrefix(Player player, Consumer<String> callback) {
        getRankAsync(player, rank -> {
            ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
            if (ranks == null) {
                plugin.getLogger().warning("Ranks section is missing in config.yml");
                callback.accept("");  // Return empty string if ranks are missing
                return;
            }

            ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
            if (rankSection != null) {
                String prefix = rankSection.getString("prefix", "");
                String coloredPrefix = ChatColor.translateAlternateColorCodes('&', prefix); // Translate color codes
                callback.accept(coloredPrefix);
            } else {
                callback.accept("");  // Default empty string if prefix is not set
            }
        });
    }


    public String getColorPreferenceSync(Player player) {
        String rank = playerCache.getOrDefault(player.getUniqueId(), "Default"); // Use your cache system
        if (rank == null) return "&f"; // Default white if rank not found

        ConfigurationSection ranksConfig = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksConfig == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return "&f";
        }

        String color = ranksConfig.getString(rank + ".name-color", "&f"); // Default to white if missing
        return ChatColor.translateAlternateColorCodes('&', color);
    }







    public String getRankPrefixSync(Player player) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        getRankPrefix(player, prefix -> future.complete(prefix)); // Complete future when prefix is available
        try {
            return future.get(5, TimeUnit.SECONDS); // Wait for the result with a timeout
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return default value in case of error or timeout
        }
    }





    public List<String> getAvailableRanks() {
        List<String> availableRanks = new ArrayList<>();
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks != null) {
            for (String rankName : ranks.getKeys(false)) {
                availableRanks.add(rankName);
            }
        }
        return availableRanks;
    }

    public boolean isValidRank(String rank) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return false;
        }
        return ranks.contains(rank);
    }

    public void getColorPreference(String rank, Consumer<String> callback) {
        ConfigurationSection ranksConfig = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksConfig != null && ranksConfig.contains(rank + ".name-color")) {
            String color = ranksConfig.getString(rank + ".name-color", "&f"); // Default color = white
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
        }, 0L, 20L); // 20L = 1 second
    }

    public void refreshPlayerDisplay(Player player) {
        getRankPrefix(player, prefix -> {
            String color = getColorPreferenceSync(player);
            String fullDisplayName = prefix + color + player.getName();
            player.setDisplayName(fullDisplayName);
            plugin.getLogger().info("Display name refreshed for " + player.getName() + " → " + fullDisplayName);
        });
    }






    private void fetchColorFromRank(String rank, Consumer<String> callback) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks != null) {
            ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
            if (rankSection != null) {
                String color = rankSection.getString("name-color", "&f"); // Default color
                callback.accept(color);
                return;
            }
        }
        callback.accept("&f"); // Default to white if color is missing
    }




    public void setMessageColorPreference(Player player, ChatColor color) {
        colorPreferences.put(player.getUniqueId(), color);
    }

    public ChatColor getMessageColorPreference(Player player) {
        return colorPreferences.getOrDefault(player.getUniqueId(), ChatColor.WHITE);
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
        ConfigurationSection ranksSection = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return new ArrayList<>();
        }


        int playerWeight = ranksSection.getConfigurationSection(playerRank).getInt("weight", 0);


        List<String> lowerRanks = new ArrayList<>();
        for (String rank : ranksSection.getKeys(false)) {
            int weight = ranksSection.getConfigurationSection(rank).getInt("weight", 0);
            if (weight < playerWeight) {
                lowerRanks.add(rank);
            }
        }
        return lowerRanks;
    }

    public void setRankAboveHead(Player player) {
        boolean showRank = plugin.getConfig().getBoolean("show-rank-above-head", true);

        if (showRank) {
            getRank(player, rank -> { // Get player rank first
                getColorPreference(rank, rankColor -> { // Then fetch color by rank name
                    String coloredName = ChatColor.translateAlternateColorCodes('&', rankColor) + player.getName();

                    player.setCustomName(coloredName);
                    player.setCustomNameVisible(true);

                    updateNameTagColor(player, rankColor);
                });
            });
        } else {
            player.setCustomName(player.getName());
            player.setCustomNameVisible(true);
            resetNameTagColor(player);
        }
    }



    public void updateNameTagColor(Player player, String rankColor) {
        String colorCode = ChatColor.translateAlternateColorCodes('&', rankColor);  // Translate & into actual color code

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(player.getName());

        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
        }

        team.setPrefix(colorCode);
        team.setSuffix("");
        team.setNameTagVisibility(NameTagVisibility.ALWAYS);

        team.addPlayer(player);
        player.setCustomName(colorCode + player.getName());  // Apply the color to the custom name
        player.setCustomNameVisible(true);

        refreshNameTag(player, team);
    }


    private void refreshNameTag(Player player, Team team) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        PlayerConnection connection = craftPlayer.getHandle().playerConnection;

        net.minecraft.server.v1_8_R3.Scoreboard nmsScoreboard = ((CraftScoreboard) Bukkit.getScoreboardManager().getMainScoreboard()).getHandle();
        net.minecraft.server.v1_8_R3.ScoreboardTeam nmsTeam = nmsScoreboard.getTeam(team.getName());

        if (nmsTeam != null) {
            connection.sendPacket(new PacketPlayOutScoreboardTeam(nmsTeam, 1));
            connection.sendPacket(new PacketPlayOutScoreboardTeam(nmsTeam, 0));
        }
    }


    private void resetNameTagColor(Player player) {

        CraftPlayer craftPlayer = (CraftPlayer) player;
        PlayerConnection connection = craftPlayer.getHandle().playerConnection;


        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team team = scoreboard.getTeam(player.getName());

        if (team == null) {
            team = scoreboard.registerNewTeam(player.getName());
        }

        team.setPrefix(ChatColor.translateAlternateColorCodes('&', "&f"));
        team.setSuffix("");

        team.setNameTagVisibility(NameTagVisibility.ALWAYS);

        net.minecraft.server.v1_8_R3.Scoreboard nmsScoreboard = ((CraftScoreboard) scoreboard).getHandle();
        net.minecraft.server.v1_8_R3.ScoreboardTeam nmsTeam = nmsScoreboard.getTeam(team.getName());


        connection.sendPacket(new PacketPlayOutScoreboardTeam(nmsTeam, 2));

        connection.sendPacket(new PacketPlayOutScoreboardTeam(nmsTeam, 0));
    }
}
