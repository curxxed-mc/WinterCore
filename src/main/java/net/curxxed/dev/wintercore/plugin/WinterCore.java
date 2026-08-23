package net.curxxed.dev.wintercore.plugin;

import lombok.Getter;
import net.curxxed.dev.wintercore.api.SimpleWinterCoreApi;
import net.curxxed.dev.wintercore.api.WinterCoreApi;
import net.curxxed.dev.wintercore.auth.AuthModule;
import net.curxxed.dev.wintercore.chat.ChatFilterService;
import net.curxxed.dev.wintercore.chat.ChatListener;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.commands.framework.BrigadierCommandHandler;
import net.curxxed.dev.wintercore.commands.framework.CommandHandler;
import net.curxxed.dev.wintercore.config.MenuConfig;
import net.curxxed.dev.wintercore.config.MessageConfig;
import net.curxxed.dev.wintercore.config.PermissionConfigManager;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.RedisSocials;
import net.curxxed.dev.wintercore.database.redis.SocialInput;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ServerStatusPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.menu.MenuManager;
import net.curxxed.dev.wintercore.menus.TagsMenu;
import net.curxxed.dev.wintercore.namemc.NameMcService;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.nms.PacketSender;
import net.curxxed.dev.wintercore.placeholders.WinterCoreExpansion;
import net.curxxed.dev.wintercore.player.BanList;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.scheduler.Tasks;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.staff.VanishService;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.util.logging.Level;

@Getter
public final class WinterCore extends JavaPlugin {

    @Getter
    private static WinterCore instance;

    private static volatile boolean shuttingDown;

    private boolean placeholderAPIEnabled = false;
    private DatabaseManager databaseManager;
    private RankManager rankManager;
    private JedisPool redisPool;
    private RedisManager redisManager;
    private RedisSocials redisSocials;
    private SocialInput socialInput;
    private TagsManager tagsManager;
    private TagsMenu tagsMenu;
    private DisguiseRegistry disguiseRegistry;
    private DisguiseEventListener disguiseEventListener;
    private PlayerService playerService;
    private MessagingService messagingService;
    private ChatListener chatListener;
    private FreezeListener freezeListener;
    private StaffModeManager staffModeManager;
    private VanishService vanishService;
    private DisguiseHandler disguiseHandler;
    private CommandHandler commandHandler;
    private AuthModule authModule;
    private MenuConfig menuConfig;
    private PermissionConfigManager permissionConfigManager;
    private NameTagColorManager nameTagColorManager;
    private StaffChatService staffChatService;
    private ChatFilterService chatFilterService;
    private MessageConfig messageConfig;
    private NetworkRedisService networkRedisService;
    private BanList banList;
    private Tasks tasks;
    private WinterCoreApi api;
    private NameMcService nameMcService;
    private PacketSender packetSender;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        shuttingDown = false;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        loadRanksFile();

        this.tasks = new Tasks(this);
        this.packetSender = new PacketSender(getLogger());
        this.messageConfig = new MessageConfig(this);
        this.chatFilterService = new ChatFilterService(this);
        this.nameMcService = new NameMcService(this);

        if (!initializeStorage()) {
            return;
        }
        initializePlaceholders();

        this.rankManager = new RankManager(this);
        this.rankManager.startAutoCacheRefresh();
        this.permissionConfigManager = new PermissionConfigManager(this);

        this.vanishService = new VanishService(this);
        this.staffModeManager = new StaffModeManager(this);
        this.disguiseRegistry = new DisguiseRegistry(this);
        this.disguiseHandler = new DefaultDisguiseHandler(this, this.disguiseRegistry);
        this.tagsManager = new TagsManager(this);
        this.tagsMenu = new TagsMenu(this, tagsManager);
        this.menuConfig = new MenuConfig(this);
        this.disguiseEventListener = new DisguiseEventListener(this, (DefaultDisguiseHandler) disguiseHandler);
        this.commandHandler = new CommandHandler(this);
        this.playerService = new PlayerService(this);
        this.messagingService = new MessagingService(this, playerService);
        this.staffChatService = new StaffChatService(this);
        this.chatListener = new ChatListener(this, tagsManager, playerService, staffChatService);
        this.freezeListener = new FreezeListener(playerService, this);
        this.banList = new BanList(this);

        MenuManager.initialize(this);
        new WinterCoreListenerRegistrar(this).register();
        this.authModule = new WinterCoreCommandRegistrar(this).register();
        registerBungee();

        this.api = new SimpleWinterCoreApi(this);
        getServer().getServicesManager().register(WinterCoreApi.class, api, this, ServicePriority.Normal);

        if (Utilities.isPaperBrigadierSupported()) {
            try {
                new BrigadierCommandHandler(this, commandHandler.getRegisteredCommands()).initialize();
                getLogger().info("Paper Brigadier command registration active.");
            } catch (ClassNotFoundException e) {
                getLogger().warning("Brigadier check passed but class resolution failed: " + e.getMessage());
            }
        }

        this.nameTagColorManager = new NameTagColorManager(this);
        this.nameTagColorManager.load();

        getLogger().info("WinterCore enabled in " + (System.currentTimeMillis() - start) + "ms.");
    }

    @Override
    public void onDisable() {
        shuttingDown = true;
        getLogger().info("Disabling WinterCore...");

        try {
            shutdownComponent("scheduled tasks", tasks == null ? null : tasks::cancelAll);
            shutdownComponent("active disguises", disguiseEventListener == null
                    ? null : disguiseEventListener::clearDisguiseOnShutdown);
            shutdownComponent("Redis offline status", () -> {
                if (redisManager != null) {
                    redisManager.publish(new ServerStatusPacket(
                            getConfig().getString("server-name", "Unknown"),
                            System.currentTimeMillis(),
                            false
                    ));
                }
            });
            shutdownComponent("online presence", () -> {
                if (redisPool != null && networkRedisService != null) {
                    for (Player online : Utilities.getOnlinePlayers()) {
                        networkRedisService.clearOnlinePresence(online.getUniqueId(), online.getName());
                    }
                }
            });
            shutdownComponent("Redis manager", redisManager == null ? null : redisManager::stop);
            shutdownComponent("database", databaseManager == null ? null : databaseManager::close);
            shutdownComponent("Redis pool", redisPool == null ? null : redisPool::close);
            shutdownComponent("name tags", nameTagColorManager == null ? null : nameTagColorManager::unload);
            shutdownComponent("API", () -> {
                if (api != null) {
                    getServer().getServicesManager().unregister(WinterCoreApi.class, api);
                    api = null;
                }
            });
        } finally {
            instance = null;
        }

        getLogger().info(CC.translate("&cWinterCore has been disabled."));
    }

    private void shutdownComponent(String name, Runnable cleanup) {
        if (cleanup == null) {
            return;
        }
        try {
            cleanup.run();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to shut down " + name, e);
        }
    }

    private boolean initializeStorage() {
        try {
            this.databaseManager = new DatabaseManager(this);
            this.databaseManager.ping();
        } catch (Exception e) {
            return disableForStorageFailure("MongoDB", e);
        }

        try {
            initializeRedis();
        } catch (Exception e) {
            return disableForStorageFailure("Redis", e);
        }
        return true;
    }

    private boolean disableForStorageFailure(String service, Exception cause) {
        getLogger().log(Level.SEVERE, "Could not connect to " + service + "; disabling WinterCore.", cause);
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    private void initializeRedis() {
        String redisHost = getConfig().getString("Redis.host", "localhost");
        int redisPort = getConfig().getInt("Redis.port", 6379);
        String redisPassword = getConfig().getString("Redis.password", "");
        int redisTimeout = Math.max(500, getConfig().getInt("Redis.timeout-ms", 2_000));
        int maxTotal = Math.max(1, getConfig().getInt("Redis.pool.max-total", 10));
        int maxIdle = Math.max(0, Math.min(maxTotal, getConfig().getInt("Redis.pool.max-idle", 5)));
        int minIdle = Math.max(0, Math.min(maxIdle, getConfig().getInt("Redis.pool.min-idle", 1)));

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(getConfig().getBoolean("Redis.pool.test-on-borrow", true));

        this.redisPool = redisPassword.isEmpty()
                ? new JedisPool(poolConfig, redisHost, redisPort, redisTimeout)
                : new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, redisPassword);

        try (Jedis jedis = redisPool.getResource()) {
            if (!"PONG".equalsIgnoreCase(jedis.ping())) {
                throw new IllegalStateException("Redis did not respond to PING");
            }
        }

        this.redisManager = new RedisManager(this);
        this.redisSocials = new RedisSocials(this);
        this.socialInput = new SocialInput(this, this.redisSocials);
        this.networkRedisService = new NetworkRedisService(this);

        redisManager.start();
        redisManager.publish(new ServerStatusPacket(
                getConfig().getString("server-name", "Unknown"),
                System.currentTimeMillis(),
                true
        ));
    }

    private void initializePlaceholders() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("PlaceholderAPI not found, some features may not work.");
            return;
        }

        getLogger().info("PlaceholderAPI found, initializing placeholders.");
        WinterCoreExpansion placeholderExpansion = new WinterCoreExpansion(this);

        if (placeholderExpansion.register()) {
            placeholderAPIEnabled = true;
            getLogger().info("WinterCore placeholders registered.");
        } else {
            getLogger().warning("Failed to register WinterCore placeholders.");
        }
    }

    private void registerBungee() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, Utilities.IS_1_13_OR_NEWER ? "minecraft:brand" : "MC|Brand");
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
    }

    private void loadRanksFile() {
        if (!new File(getDataFolder(), "ranks.yml").exists()) {
            saveResource("ranks.yml", false);
        }
    }

    public NetworkRedisService getNetworkRedisService() {
        if (networkRedisService == null) {
            throw new IllegalStateException("NetworkRedisService not initialized");
        }
        return networkRedisService;
    }

    /** @deprecated use {@link #getNetworkRedisService()} */
    @Deprecated
    public NetworkRedisService getNRS() {
        return getNetworkRedisService();
    }

    public static boolean isShuttingDown() {
        return shuttingDown;
    }
}
