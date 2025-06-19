package net.curxxed.dev.icore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.curxxed.dev.icore.plugin.iCore;
import lombok.Getter;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {

    @Getter
    private static DatabaseManager instance;

    private final iCore plugin;
    private HikariDataSource dataSource;
    public static RankCache rankCache = new RankCache();

    public DatabaseManager(iCore plugin) {
        this.plugin = plugin;
        setupDataSource();
        createTables();
    }

    public static DatabaseManager init(iCore plugin) {
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

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ICoreHikariPool");

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("✅ HikariCP pool initialized.");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void createTables() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sqlRanks = "CREATE TABLE IF NOT EXISTS player_ranks (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "rank VARCHAR(50) NOT NULL DEFAULT 'Default'" +
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

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlRanks);
                stmt.executeUpdate(sqlBans);
                stmt.executeUpdate(sqlMutes);
                stmt.executeUpdate(sqlWarnings);
                stmt.executeUpdate(sqlKicks);
                stmt.executeUpdate(sqlTags);
                plugin.getLogger().info("📄 MySQL tables are ready!");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error creating MySQL tables!", e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not set tag for " + uuid, e);
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
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(tag));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch tag for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not update rank for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch rank for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not ban player " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not unban player " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not check ban status for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch ban reason for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not check mute status for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not unmute player " + uuid, e);
            }
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("🔌 HikariCP pool shut down.");
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not add warning for " + playerName, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not mute player " + targetUUID, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not increment kick count for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch warnings for " + playerName, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch mutes for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch kicks for " + playerName, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(kicks));
        });
    }

    public void getBans(String playerName, Consumer<List<Map<String, String>>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map<String, String>> bans = new ArrayList<>();
            String sql = "SELECT reason, banned_at FROM player_bans WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> ban = new HashMap<>();
                        ban.put("reason", rs.getString("reason"));
                        ban.put("date", new java.util.Date(rs.getLong("banned_at")).toString());
                        bans.add(ban);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch bans for " + playerName, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not remove expired bans", e);
            }
        });
    }

    public void getUUIDByName(String playerName, Consumer<UUID> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT uuid FROM player_ranks WHERE rank = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerName);
                try (ResultSet rs = ps.executeQuery()) {
                    UUID uuid = rs.next() ? UUID.fromString(rs.getString("uuid")) : null;
                    plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(uuid));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch UUID for player " + playerName, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch ban details for " + uuid, e);
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
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch mute details for " + uuid, e);
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
}
