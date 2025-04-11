package hyp.ilfov.i.icore.Database;

import hyp.ilfov.i.icore.Main;
import lombok.Getter;

import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

@Getter
public class DatabaseManager {
    private final Main plugin;
    private Connection connection;

    private final String host, database, username, password;
    private final int port;
    @Getter
    public static DatabaseManager instance;


    public static RankCache rankCache = new RankCache();

    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("mysql.port", 3306);
        this.database = plugin.getConfig().getString("mysql.database", "minecraft");
        this.username = plugin.getConfig().getString("mysql.username", "root");
        this.password = plugin.getConfig().getString("mysql.password", "password");
        connect();

        if (connection != null) {
            createTables();
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::checkConnection, 6000L, 6000L);
        }
    }



    private synchronized void connect() {
        try {
            if (connection != null && !connection.isClosed()) return;

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("✅ Connected to MySQL!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Could not connect to MySQL!", e);
        }
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error checking MySQL connection!", e);
            connect();
        }
        return connection;
    }

    private void checkConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().warning("⚠️ MySQL connection lost! Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error checking MySQL connection!", e);
        }
    }

    public void createTables() {
        if (connection == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS player_ranks (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "rank VARCHAR(50) NOT NULL DEFAULT 'Default'" +
                ");";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
            plugin.getLogger().info("📄 MySQL tables are ready!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error creating MySQL tables!", e);
        }
    }

    public void setRank(UUID uuid, String rank) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_ranks (uuid, rank) VALUES (?, ?) ON DUPLICATE KEY UPDATE rank = ?;";
            try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rank);
                ps.setString(3, rank);
                ps.executeUpdate();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not update rank for " + uuid, e);
            }
        });
    }

    public void getRank(UUID uuid, Consumer<String> callback) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT rank FROM player_ranks WHERE uuid = ?;";
            try (PreparedStatement ps = this.connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    String rank = rs.next() ? rs.getString("rank") : "Default";
                    this.plugin.getServer().getScheduler().runTask(plugin, () -> {
                        rankCache.set(uuid, rank);
                        callback.accept(rank);
                    });
                }
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not fetch rank for " + uuid, e);
                this.plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept("Default"));
            }
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("🔌 Disconnected from MySQL.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "❌ Error closing MySQL connection!", e);
        }
    }
}