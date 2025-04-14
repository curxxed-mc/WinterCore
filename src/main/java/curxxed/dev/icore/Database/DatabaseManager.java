package curxxed.dev.icore.Database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import curxxed.dev.icore.Main;
import lombok.Getter;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {

    @Getter
    private static DatabaseManager instance;

    private final Main plugin;
    private HikariDataSource dataSource;
    public static RankCache rankCache = new RankCache();

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        setupDataSource();
        createTables();
    }

    public static DatabaseManager init(Main plugin) {
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

            String sqlPermissions = "CREATE TABLE IF NOT EXISTS player_permissions (" +
                    "uuid VARCHAR(36), " +
                    "permission VARCHAR(100), " +
                    "PRIMARY KEY(uuid, permission)" +
                    ");";

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlRanks);
                stmt.executeUpdate(sqlPermissions);
                plugin.getLogger().info("📄 MySQL tables are ready!");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Error creating MySQL tables!", e);
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

    public void addPermission(UUID uuid, String permission) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT IGNORE INTO player_permissions (uuid, permission) VALUES (?, ?);";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, permission);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not add permission " + permission + " to " + uuid, e);
            }
        });
    }

    public void removePermission(UUID uuid, String permission) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM player_permissions WHERE uuid = ? AND permission = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, permission);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not remove permission " + permission + " from " + uuid, e);
            }
        });
    }

    public void getPlayerPermissions(UUID uuid, Consumer<List<String>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> permissions = new ArrayList<>();
            String sql = "SELECT permission FROM player_permissions WHERE uuid = ?;";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        permissions.add(rs.getString("permission"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "❌ Could not fetch permissions for " + uuid, e);
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(permissions));
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("🔌 HikariCP pool shut down.");
        }
    }
}
