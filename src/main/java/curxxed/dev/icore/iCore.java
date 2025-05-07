package curxxed.dev.icore;

import com.google.gson.Gson;
import curxxed.dev.icore.Commands.Staff.FreezeCommand;
import curxxed.dev.icore.Database.DatabaseManager;
import curxxed.dev.icore.Database.RedisManager;
import curxxed.dev.icore.Placeholders.Placeholder;
import curxxed.dev.icore.utils.GUI.ColorGUI;
import curxxed.dev.icore.listeners.PlayerListener;
import curxxed.dev.icore.utils.PermissionManager;
import curxxed.dev.icore.utils.RankManager;
import curxxed.dev.icore.utils.RegisterPlugin;
import lombok.Getter;
import lombok.Setter;
import lombok.var;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class iCore extends JavaPlugin {

    public static iCore INSTANCE;
    public Set<UUID> vanished_players = new HashSet<>();
    @Getter
    private RankManager rankManager;
    @Getter
    private ColorGUI colorGUI;
    @Getter
    private JedisPool redisPool;
    @Getter
    private RedisManager redisManager;
    @Getter
    private FreezeCommand freezeCommand;
    @Getter
    public DatabaseManager databaseManager;
    @Getter
    private PermissionManager permissionManager;
    @Setter
    private Plugin plugin;
    @Setter
    @Getter
    private Gson gson;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        loadRanksFile();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);

        new Placeholder(this).register();

        this.databaseManager = DatabaseManager.init(this);


        this.rankManager = new RankManager(this);
        this.colorGUI = new ColorGUI(this);
        String redisHost = getConfig().getString("Redis.host", "localhost");
        int redisPort = getConfig().getInt("Redis.port", 6379);
        String redisPassword = getConfig().getString("Redis.password", "");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10); // Maximum number of connections
        poolConfig.setMaxIdle(5);  // Maximum idle connections
        poolConfig.setMinIdle(1);  // Minimum idle connections
        poolConfig.setTestOnBorrow(true);


        if (redisPassword.isEmpty()) {
            this.redisPool = new JedisPool(poolConfig, redisHost, redisPort);
        } else {
            this.redisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
        }
        this.redisManager = new RedisManager(this);
        redisManager.startHeartbeatSender();     // Start sending first
        redisManager.startHeartbeatMonitor();    // Then monitor others
        redisManager.publishServerStatus(true);  // Then announce "I'm online"
        redisManager.startInfoUpdater();
        RegisterPlugin registerPlugin = new RegisterPlugin();
        permissionManager = new PermissionManager(this);
        registerPlugin.registerPlugin(this);
        rankManager.startAutoCacheRefresh();
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.removeExpiredBans();
        }, 0L, 20L);
        getLogger().info("iCore has been enabled in " + (System.currentTimeMillis() - start) + "ms!");

    }




    @Override
    public void onDisable() {
        // Log to verify if the onDisable() method is called
        getLogger().info("Disabling server, publishing offline status");

        // Publish the server status asynchronously to Redis to avoid blocking shutdown
        try {
            redisManager.publishServerStatus(false);
        } catch (NullPointerException e) {
            getLogger().warning("RedisManager was not initialized, could not publish offline status.");
        }

        try (var jedis = redisPool.getResource()) {
            String key = "server:" + getConfig().getString("server-name", "unknown") + ":heartbeat";
            jedis.del(key);
        } catch (Exception e) {
            getLogger().warning("Failed to remove heartbeat key: " + e.getMessage());
        }

        // Proceed with other cleanup tasks after the status is published
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (redisPool != null) {
            redisPool.close();
        }
        rankManager.closeDatabaseConnection();
        saveVanishedPlayers();
        getLogger().info("§ciCore has been disabled.");
    }


    public PlayerListener getPlayerListener() {
        return new PlayerListener(this);
    }

    public void saveVanishedPlayers() {
        Set<String> vanishedUUIDs = new HashSet<>();
        for (UUID uuid : vanished_players) {
            vanishedUUIDs.add(uuid.toString());
        }
        getConfig().set("vanished_players", vanishedUUIDs);
        saveConfig();
    }

    public void loadRanksFile() {
        File ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            saveResource("ranks.yml", false);
        }
        YamlConfiguration ranksConfig = YamlConfiguration.loadConfiguration(ranksFile);
        getLogger().info("Ranks file loaded successfully.");
    }

    public Placeholder getPlaceholders() {
        return new Placeholder(this);
    }
}
