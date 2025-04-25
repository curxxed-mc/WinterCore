package curxxed.dev.icore.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.NMSUtils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.lang.reflect.*;

import static curxxed.dev.icore.utils.NMSUtils.sendPacket;


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
        try {
            // Update the rank in the database
            databaseManager.setRank(player.getUniqueId(), rank);

            // Send rank update to BungeeCord
            sendRankUpdateToBungee(player.getName(), rank);

            // Invalidate the player's rank cache
            invalidatePlayerCache(player);

            // Refresh the player's display name and rank
            getRankAsync(player, fetchedRank -> {
                plugin.getLogger().info("Cache refreshed automatically after rank set for " + player.getName());
                refreshPlayerDisplay(player);
            });

            // Notify the player and the giver
            player.sendMessage(ChatColor.GREEN + "Your rank has been set to " + rank + " by " + giver.getName() + ".");
            giver.sendMessage(ChatColor.GREEN + "You have successfully set the rank of " + player.getName() + " to " + rank + ".");

            // Update the player's rank and name tag
            updatePlayerRank(player);
            setRankAboveHead(player);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to set rank for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void invalidatePlayerCache(Player player) {
        playerRanks.remove(player.getUniqueId());
        getRankAsync(player, rank -> refreshPlayerDisplay(player));
    }

    public void createRank(String rankName) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            ranks = plugin.getConfig().createSection("ranks");
        }

        if (ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank already exists!");
        }

        ConfigurationSection newRank = ranks.createSection(rankName);
        newRank.set("prefix", "");
        newRank.set("weight", 1);
        newRank.set("name-color", "&f");

        plugin.saveConfig();
    }

    public void deleteRank(String rankName) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }

        ranks.set(rankName, null);
        plugin.saveConfig();
    }

    public void addPermission(String rankName, String permission) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }

        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        List<String> permissions = rankSection.getStringList("permissions");
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            rankSection.set("permissions", permissions);
            plugin.saveConfig();
        }
    }

    public void removePermission(String rankName, String permission, boolean inherit) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }

        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        List<String> permissions = rankSection.getStringList("permissions");
        if (permissions.contains(permission)) {
            permissions.remove(permission);
            rankSection.set("permissions", permissions);
            plugin.saveConfig();
        }

        if (inherit) {
            List<String> inherits = rankSection.getStringList("inherits");
            for (String inheritedRank : inherits) {
                removePermission(inheritedRank, permission, true);
            }
        }
    }

    public void setPrefix(String rankName, String prefix) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }

        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        rankSection.set("prefix", prefix);
        plugin.saveConfig();
    }

    public List<String> getPermissions(String rankName) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }

        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        return rankSection.getStringList("permissions");
    }

    public void setColor(String rankName, String color) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }
        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        rankSection.set("color", color);
        plugin.saveConfig();
    }

    public void setWeight(String rankName, int weight) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null || !ranks.contains(rankName)) {
            throw new IllegalArgumentException("Rank does not exist!");
        }
        ConfigurationSection rankSection = ranks.getConfigurationSection(rankName);
        rankSection.set("weight", weight);
        plugin.saveConfig();
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
                playerRanks.put(player.getUniqueId(), rank);
                future.complete(rank);
            });

            try {
                return future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
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
            if (rank == null) {
                player.setDisplayName(ChatColor.GRAY + player.getName());
                player.setPlayerListName(ChatColor.GRAY + player.getName());
                return;
            }
            plugin.getPermissionManager().clearPermissions(player);
            List<String> permissions = getPermissionsForRank(rank);
            PermissionAttachment attachment = plugin.getPermissionManager().getOrCreateAttachment(player);
            for (String permission : permissions) {
                attachment.setPermission(permission, true);
            }
            String nameColor = plugin.getConfig().getString("ranks." + rank + ".name-color", "&f");
            String coloredName = ChatColor.translateAlternateColorCodes('&', nameColor) + player.getName() + ChatColor.RESET;

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
        String rank = getRankSync(player);
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return "";
        }

        ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
        if (rankSection != null) {
            String prefix = rankSection.getString("prefix", "");
            return ChatColor.translateAlternateColorCodes('&', prefix);
        } else {
            return "";
        }
    }



    public void getRankPrefix(Player player, Consumer<String> callback) {
        getRankAsync(player, rank -> {
            ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
            if (ranks == null) {
                plugin.getLogger().warning("Ranks section is missing in config.yml");
                callback.accept("");
                return;
            }

            ConfigurationSection rankSection = ranks.getConfigurationSection(rank);
            if (rankSection != null) {
                String prefix = rankSection.getString("prefix", "");
                String coloredPrefix = ChatColor.translateAlternateColorCodes('&', prefix);
                callback.accept(coloredPrefix);
            } else {
                callback.accept("");
            }
        });
    }


    public String getColorPreferenceSync(Player player) {
        String rank = playerCache.getOrDefault(player.getUniqueId(), "Default");
        if (rank == null) return "&f";

        ConfigurationSection ranksConfig = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksConfig == null) {
            plugin.getLogger().warning("Ranks section is missing in config.yml");
            return "&f";
        }

        String color = ranksConfig.getString(rank + ".name-color", "&f");
        return ChatColor.translateAlternateColorCodes('&', color);
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
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks != null) {
            availableRanks.addAll(ranks.getKeys(false));
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
            String color = ranksConfig.getString(rank + ".name-color", "&f");
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
        getRankPrefix(player, prefix -> {
            String color = getColorPreferenceSync(player);
            String fullDisplayName = prefix + " " + color + player.getName();
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
        callback.accept("&f");
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
            getRank(player, rank -> getColorPreference(rank, rankColor -> {
                String coloredName = ChatColor.translateAlternateColorCodes('&', rankColor) + player.getName();

                player.setCustomName(coloredName);
                player.setCustomNameVisible(true);

                updateNameTagColor(player, rankColor);
            }));
        } else {
            player.setCustomName(player.getName());
            player.setCustomNameVisible(true);
            resetNameTagColor(player);
        }
    }



    public void updateNameTagColor(Player player, String rankColor) {
        try {
            String teamName = player.getName();

            Class<?> scoreboardTeamClass = NMSUtils.getNMSClass("ScoreboardTeam");
            if (NMSUtils.IS_LEGACY) {
                // Legacy NMS logic
                Object scoreboardTeam = scoreboardTeamClass.getConstructor(String.class).newInstance(teamName);

                Method setPrefixMethod = scoreboardTeamClass.getMethod("setPrefix", String.class);
                Method setSuffixMethod = scoreboardTeamClass.getMethod("setSuffix", String.class);
                setPrefixMethod.invoke(scoreboardTeam, ChatColor.translateAlternateColorCodes('&', rankColor));
                setSuffixMethod.invoke(scoreboardTeam, "");

                Class<?> enumChatFormatClass = NMSUtils.getNMSClass("EnumChatFormat");
                Object enumChatFormat = Enum.valueOf((Class<Enum>) enumChatFormatClass, NMSUtils.getEnumNameFromColorCode(rankColor.charAt(1)));
                Method setColorMethod = scoreboardTeamClass.getMethod("setColor", enumChatFormatClass);
                setColorMethod.invoke(scoreboardTeam, enumChatFormat);

                Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutScoreboardTeam");
                Object packetCreate = NMSUtils.createInstance(packetClass, scoreboardTeam, 0);
                Object packetUpdate = NMSUtils.createInstance(packetClass, scoreboardTeam, 2);
                NMSUtils.sendPacket(player, packetCreate);
                NMSUtils.sendPacket(player, packetUpdate);

            } else {
                // Modern NMS logic
                Class<?> scoreboardClass = NMSUtils.getNMSClass("Scoreboard");
                Class<?> enumChatFormatClass = NMSUtils.getNMSClass("EnumChatFormat");
                Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutScoreboardTeam");

                Object scoreboard = scoreboardClass.getDeclaredConstructor().newInstance();
                Constructor<?> teamConstructor = scoreboardTeamClass.getConstructor(scoreboardClass, String.class);
                Object scoreboardTeam = teamConstructor.newInstance(scoreboard, teamName);

                Method setPrefixMethod = scoreboardTeamClass.getMethod("setPrefix", String.class);
                Method setSuffixMethod = scoreboardTeamClass.getMethod("setSuffix", String.class);
                setPrefixMethod.invoke(scoreboardTeam, ChatColor.translateAlternateColorCodes('&', rankColor));
                setSuffixMethod.invoke(scoreboardTeam, "");

                Object enumChatFormat = Enum.valueOf((Class<Enum>) enumChatFormatClass, NMSUtils.getEnumNameFromColorCode(rankColor.charAt(1)));
                Method setColorMethod = scoreboardTeamClass.getMethod("setColor", enumChatFormatClass);
                setColorMethod.invoke(scoreboardTeam, enumChatFormat);

                Object packetCreate = NMSUtils.createInstance(packetClass, scoreboardTeam, 0);
                Object packetUpdate = NMSUtils.createInstance(packetClass, scoreboardTeam, 2);
                NMSUtils.sendPacket(player, packetCreate);
                NMSUtils.sendPacket(player, packetUpdate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void refreshNameTag(Player player, Team team) {
        try {
            Object nmsScoreboard = NMSUtils.getCraftBukkitClass("scoreboard.CraftScoreboard")
                    .getMethod("getHandle")
                    .invoke(Bukkit.getScoreboardManager().getMainScoreboard());

            Object nmsTeam = nmsScoreboard.getClass()
                    .getMethod("getTeam", String.class)
                    .invoke(nmsScoreboard, team.getName());

            if (nmsTeam != null) {
                Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutScoreboardTeam");
                Object packet1 = NMSUtils.createInstance(packetClass, nmsTeam, 1);
                Object packet2 = NMSUtils.createInstance(packetClass, nmsTeam, 0);
                NMSUtils.sendPacket(player, packet1);
                NMSUtils.sendPacket(player, packet2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetNameTagColor(Player player) {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(player.getName());

            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }

            team.setPrefix(ChatColor.translateAlternateColorCodes('&', "&f"));
            team.setSuffix("");
            team.setNameTagVisibility(NameTagVisibility.ALWAYS);

            if (NMSUtils.IS_LEGACY) {
                Object nmsScoreboard = NMSUtils.getCraftBukkitClass("scoreboard.CraftScoreboard")
                        .getMethod("getHandle")
                        .invoke(Bukkit.getScoreboardManager().getMainScoreboard());

                Object nmsTeam = nmsScoreboard.getClass()
                        .getMethod("getTeam", String.class)
                        .invoke(nmsScoreboard, team.getName());

                if (nmsTeam != null) {
                    Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutScoreboardTeam");
                    Object packet1 = NMSUtils.createInstance(packetClass, nmsTeam, 2);
                    Object packet2 = NMSUtils.createInstance(packetClass, nmsTeam, 0);
                    NMSUtils.sendPacket(player, packet1);
                    NMSUtils.sendPacket(player, packet2);
                }
            } else {
                Class<?> scoreboardTeamClass = NMSUtils.getNMSClass("ScoreboardTeam");
                Object nmsTeam = scoreboardTeamClass.getConstructor(String.class).newInstance(team.getName());

                Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutScoreboardTeam");
                Object packet1 = NMSUtils.createInstance(packetClass, nmsTeam, 2);
                Object packet2 = NMSUtils.createInstance(packetClass, nmsTeam, 0);
                NMSUtils.sendPacket(player, packet1);
                NMSUtils.sendPacket(player, packet2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
