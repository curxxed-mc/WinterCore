package net.curxxed.dev.wintercore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {

    @Getter
    private static DatabaseManager instance;

    private final WinterCore plugin;
    private HikariDataSource dataSource;
    public static RankCache rankCache = new RankCache();
    public static TagCache tagCache = new TagCache();

    public DatabaseManager(WinterCore plugin) {
        this.plugin = plugin;
        setupDataSource();
        createTables();
    }

    public static DatabaseManager init(WinterCore plugin) {
        instance = new DatabaseManager(plugin);
        return instance;
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String database = plugin.getConfig().getString("mysql.database", "minecraft");
        String username = plugin.getConfig().getString("mysql.username", "root");
        String password = plugin.getConfig().getString("mysql.password", "password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setPoolName("WinterCoreHikariPool");

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("HikariCP pool initialized.");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createTables() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sqlRanks = "CREATE TABLE IF NOT EXISTS player_ranks (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "rank VARCHAR(50) NOT NULL DEFAULT 'Default', " +
                    "granted_by VARCHAR(36), " +
                    "granted_at BIGINT, " +
                    "expires_at BIGINT, " +
                    "reason VARCHAR(255)" +
                    ");";
            String sqlBans = "CREATE TABLE IF NOT EXISTS player_bans (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "reason VARCHAR(255), " +
                    "banned_at BIGINT, " +
                    "expiration BIGINT" + // Add the expiration column here
                    ");";

            String sqlMutes = "CREATE TABLE IF NOT EXISTS player_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "reason VARCHAR(255), " +
                    "issuer VARCHAR(50), " +
                    "expiration BIGINT" +
                    ");";

            String sqlWarnings = "CREATE TABLE IF NOT EXISTS player_warnings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_name VARCHAR(50), " +
                    "reason VARCHAR(255), " +
                    "issuer VARCHAR(50), " +
                    "warned_at BIGINT" +
                    ");";

            String sqlKicks = "CREATE TABLE IF NOT EXISTS player_kicks (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "kick_count INT NOT NULL DEFAULT 0" +
                    ");";
            String sqlTags = "CREATE TABLE IF NOT EXISTS player_tags (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "tag VARCHAR(50)" +
                    ");";

            // New table for player IPs
            String sqlPlayerIPs = "CREATE TABLE IF NOT EXISTS player_ips (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "ip VARCHAR(45) NOT NULL, " +
                    "last_seen BIGINT, " +
                    "PRIMARY KEY (uuid, ip)" +
                    ");";

            // New table for rank grants history
            String sqlRankGrants = "CREATE TABLE IF NOT EXISTS player_rank_grants (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "rank VARCHAR(50) NOT NULL, " +
                    "granted_by VARCHAR(36) NOT NULL, " +
                    "granted_at BIGINT NOT NULL, " +
                    "expires_at BIGINT, " +
                    "reason VARCHAR(255)" +
                    ");";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlRanks);
                stmt.executeUpdate(sqlBans);
                stmt.executeUpdate(sqlMutes);
                stmt.executeUpdate(sqlWarnings);
                stmt.executeUpdate(sqlKicks);
                stmt.executeUpdate(sqlTags);
                stmt.executeUpdate(sqlPlayerIPs);
                stmt.executeUpdate(sqlRankGrants);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error creating MySQL tables!", e);
            }

            // --- Ensure player_ranks columns exist (auto-migrate) ---
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM player_ranks LIKE 'granted_by'");
                if (!rs.next()) stmt.executeUpdate("ALTER TABLE player_ranks ADD COLUMN granted_by VARCHAR(36)");
                rs = stmt.executeQuery("SHOW COLUMNS FROM player_ranks LIKE 'granted_at'");
                if (!rs.next()) stmt.executeUpdate("ALTER TABLE player_ranks ADD COLUMN granted_at BIGINT");
                rs = stmt.executeQuery("SHOW COLUMNS FROM player_ranks LIKE 'expires_at'");
                if (!rs.next()) stmt.executeUpdate("ALTER TABLE player_ranks ADD COLUMN expires_at BIGINT");
                rs = stmt.executeQuery("SHOW COLUMNS FROM player_ranks LIKE 'reason'");
                if (!rs.next()) stmt.executeUpdate("ALTER TABLE player_ranks ADD COLUMN reason VARCHAR(255)");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error auto-migrating player_ranks columns!", e);
            }
        });
    }

    public void setPlayerTag(UUID uuid, String tag) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_tags (uuid, tag) VALUES (?, ?) ON DUPLICATE KEY UPDATE tag = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, tag);
                ps.setString(3, tag);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not set tag for " + uuid, e);
            }
        });
    }

    public void getPlayerTag(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT tag FROM player_tags WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    String tag = rs.next() ? rs.getString("tag") : null;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        tagCache.set(uuid, tag); // cache the tag
                        callback.accept(tag);
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch tag for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void setRank(UUID uuid, String rank) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_ranks (uuid, rank) VALUES (?, ?) ON DUPLICATE KEY UPDATE rank = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rank);
                ps.setString(3, rank);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank for " + uuid, e);
            }
        });
    }

    // Set rank with metadata (current grant)
    public void setRankWithMeta(UUID uuid, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_ranks (uuid, rank, granted_by, granted_at, expires_at, reason) VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE rank = VALUES(rank), granted_by = VALUES(granted_by), granted_at = VALUES(granted_at), expires_at = VALUES(expires_at), reason = VALUES(reason);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rank);
                ps.setString(3, grantedBy.toString());
                ps.setLong(4, grantedAt);
                if (expiresAt != null) {
                    ps.setLong(5, expiresAt);
                } else {
                    ps.setNull(5, java.sql.Types.BIGINT);
                }
                ps.setString(6, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not update rank with meta for " + uuid, e);
            }
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT rank FROM player_ranks WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    String rank = rs.next() ? rs.getString("rank") : "Default";
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        rankCache.set(uuid, rank);
                        callback.accept(rank);
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch rank for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept("Default"));
            }
        });
    }

    public void banPlayer(UUID uuid, String reason, Instant expiration) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_bans (uuid, reason, banned_at, expiration) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE reason = ?, banned_at = ?, expiration = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                ps.setString(1, uuid.toString());
                ps.setString(2, reason);
                ps.setLong(3, now);
                ps.setObject(4, expiration != null ? expiration.toEpochMilli() : null);
                ps.setString(5, reason);
                ps.setLong(6, now);
                ps.setObject(7, expiration != null ? expiration.toEpochMilli() : null);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not ban player " + uuid, e);
            }
        });
    }

    public void unbanPlayer(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unban player " + uuid, e);
            }
        });
    }

    public void isPlayerBanned(UUID uuid, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT uuid FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    boolean banned = rs.next();
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(banned));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not check ban status for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void getBanReason(UUID uuid, Consumer<String> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT reason FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    String reason = rs.next() ? rs.getString("reason") : null;
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(reason));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch ban reason for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void isPlayerMuted(UUID uuid, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT COUNT(*) FROM player_mutes WHERE uuid = ? AND (expiration IS NULL OR expiration > ?);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    boolean isMuted = rs.next() && rs.getInt(1) > 0;
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(isMuted));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not check mute status for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    public void unmutePlayer(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_mutes WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not unmute player " + uuid, e);
            }
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP pool shut down.");
        }
    }

    public void addWarning(String playerName, String reason, String issuer) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_warnings (player_name, reason, issuer, warned_at) VALUES (?, ?, ?, ?);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                ps.setString(2, reason);
                ps.setString(3, issuer);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add warning for " + playerName, e);
            }
        });
    }

    public void mutePlayer(UUID targetUUID, String reason, String issuer, Instant expirationTime) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_mutes (uuid, reason, issuer, expiration) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE reason = ?, issuer = ?, expiration = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUUID.toString());
                ps.setString(2, reason);
                ps.setString(3, issuer);
                ps.setObject(4, expirationTime != null ? expirationTime.toEpochMilli() : null);
                ps.setString(5, reason);
                ps.setString(6, issuer);
                ps.setObject(7, expirationTime != null ? expirationTime.toEpochMilli() : null);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not mute player " + targetUUID, e);
            }
        });
    }

    public void incrementKickCount(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_kicks (uuid, kick_count) VALUES (?, 1) " +
                    "ON DUPLICATE KEY UPDATE kick_count = kick_count + 1;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not increment kick count for " + uuid, e);
            }
        });
    }

    public void getWarnings(String playerName, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> warnings = new ArrayList<>();
            String sql = "SELECT reason, issuer, warned_at FROM player_warnings WHERE player_name = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> warning = new HashMap<>();
                        warning.put("reason", rs.getString("reason"));
                        warning.put("issuer", rs.getString("issuer"));
                        warning.put("date", new java.util.Date(rs.getLong("warned_at")).toString());
                        warnings.add(warning);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch warnings for " + playerName, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(warnings));
        });
    }

    public void getMutes(UUID uuid, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> mutes = new ArrayList<>();
            String sql = "SELECT reason, issuer, expiration FROM player_mutes WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> mute = new HashMap<>();
                        mute.put("reason", rs.getString("reason"));
                        mute.put("issuer", rs.getString("issuer"));
                        mute.put("expiration", rs.getString("expiration"));
                        mutes.add(mute);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch mutes for " + uuid, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(mutes));
        });
    }

    public void getKicks(String playerName, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> kicks = new ArrayList<>();
            String sql = "SELECT kick_count FROM player_kicks WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> kick = new HashMap<>();
                        kick.put("count", String.valueOf(rs.getInt("kick_count")));
                        kicks.add(kick);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch kicks for " + playerName, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(kicks));
        });
    }

    public void getBans(String playerName, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> bans = new ArrayList<>();
            String sql = "SELECT reason, banned_at, expiration FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> ban = new HashMap<>();
                        ban.put("reason", rs.getString("reason"));
                        ban.put("date", new java.util.Date(rs.getLong("banned_at")).toString());
                        ban.put("expiration", rs.getString("expiration"));
                        bans.add(ban);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch bans for " + playerName, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(bans));
        });
    }

    public void removeExpiredBans() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_bans WHERE expiration IS NOT NULL AND expiration <= ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove expired bans", e);
            }
        });
    }

    // Get UUID by player name using Bukkit's OfflinePlayer (supports offline players)
    public void getUUIDByName(String playerName, Consumer<UUID> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID uuid = null;
            try {
                uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception ignored) {}
            final UUID result = uuid;
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }
    public void getBanDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT expiration, reason FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, Object> banDetails = new HashMap<>();
                    if (rs.next()) {
                        banDetails.put("expiration", rs.getObject("expiration")); // Can be null
                        banDetails.put("reason", rs.getString("reason"));
                    }
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(banDetails));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch ban details for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    public void getMuteDetails(UUID uuid, Consumer<Map<String, Object>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT reason, expiration FROM player_mutes WHERE uuid = ? AND (expiration IS NULL OR expiration > ?);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, Object> muteDetails = new HashMap<>();
                    if (rs.next()) {
                        muteDetails.put("reason", rs.getString("reason"));
                        muteDetails.put("expiration", rs.getObject("expiration"));
                    }
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(muteDetails));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch mute details for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    /**
     * Record a player's IP address (call on join).
     */
    public void recordPlayerIP(UUID uuid, String ip) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_ips (uuid, ip, last_seen) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                ps.setString(1, uuid.toString());
                ps.setString(2, ip);
                ps.setLong(3, now);
                ps.setLong(4, now);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not record IP for " + uuid, e);
            }
        });
    }

    /**
     * Get all IPs a player has used.
     */
    public void getIPsForUUID(UUID uuid, Consumer<Set<String>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> ips = new HashSet<>();
            String sql = "SELECT ip FROM player_ips WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ips.add(rs.getString("ip"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch IPs for " + uuid, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(ips));
        });
    }

    /**
     * Get all UUIDs that have joined from a given IP.
     */
    public void getUUIDsForIP(String ip, Consumer<Set<UUID>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> uuids = new HashSet<>();
            String sql = "SELECT uuid FROM player_ips WHERE ip = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ip);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            uuids.add(UUID.fromString(rs.getString("uuid")));
                        } catch (IllegalArgumentException ignore) {}
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not fetch UUIDs for IP " + ip, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(uuids));
        });
    }

    /**
     * Get all UUIDs that have ever joined from any IP a target UUID has used (alts).
     */
    public void getAlts(UUID uuid, Consumer<Set<UUID>> callback) {
        getIPsForUUID(uuid, ips -> {
            if (ips.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(Collections.singleton(uuid)));
                return;
            }
            Set<UUID> allAlts = new HashSet<>();
            // For each IP, get all UUIDs
            final int[] remaining = {ips.size()};
            for (String ip : ips) {
                getUUIDsForIP(ip, uuids -> {
                    allAlts.addAll(uuids);
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(allAlts));
                      }
                  });
              }
          });
      }

    public static class RankCache {

        private final HashMap<UUID, String> cache = new HashMap<>();

        public void set(UUID uuid, String rank) {
            cache.put(uuid, rank);
        }

        public String get(UUID uuid) {
            return cache.getOrDefault(uuid, "Default");
        }

        public boolean has(UUID uuid) {
            return cache.containsKey(uuid);
        }
    }

    public static class TagCache {
        private final HashMap<UUID, String> cache = new HashMap<>();

        public void set(UUID uuid, String tag) {
            cache.put(uuid, tag);
        }

        public String get(UUID uuid) {
            return cache.getOrDefault(uuid, null);
        }

        public boolean has(UUID uuid) {
            return cache.containsKey(uuid);
        }
    }

    // Store a rank grant in the database
    public void addRankGrant(UUID targetUUID, String rank, UUID grantedBy, long grantedAt, Long expiresAt, String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_rank_grants (uuid, rank, granted_by, granted_at, expires_at, reason) VALUES (?, ?, ?, ?, ?, ?);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUUID.toString());
                ps.setString(2, rank);
                ps.setString(3, grantedBy.toString());
                ps.setLong(4, grantedAt);
                if (expiresAt != null) {
                    ps.setLong(5, expiresAt);
                } else {
                    ps.setNull(5, java.sql.Types.BIGINT);
                }
                ps.setString(6, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not add rank grant for " + targetUUID, e);
            }
        });
    }

    // Remove a warning by its ID
    public void removeWarning(int warningId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_warnings WHERE id = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, warningId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove warning with id " + warningId, e);
            }
        });
    }

    // Remove a mute by UUID
    public void removeMute(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_mutes WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove mute for uuid " + uuid, e);
            }
        });
    }

    // Remove a ban by UUID
    public void removeBan(UUID uuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove ban for uuid " + uuid, e);
            }
        });
    }

    // Remove a rank grant by its ID
    public void removeRankGrant(int grantId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_rank_grants WHERE id = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, grantId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not remove rank grant with id " + grantId, e);
            }
        });
    }
}
