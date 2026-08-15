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
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
public final class WinterCore extends JavaPlugin {

    @Getter
    private static WinterCore instance;

    public static volatile boolean isShuttingDown = false;

    private boolean placeholderAPIEnabled = false;
    private final ConcurrentMap<UUID, DisguiseData> disguiseDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

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
    PlayerService playerService;
    MessagingService messagingService;
    ChatListener chatListener;
    FreezeListener freezeListener;
    private StaffModeManager staffModeManager;
    private DisguiseHandler disguiseHandler;
    private CommandHandler commandHandler;
    private AuthModule authModule;
    private MenuConfig menuConfig;
    private PermissionConfigManager permissionConfigManager;
    private NameTagColorManager nameTagColorManager;
    StaffChatService staffChatService;
    private ChatFilterService chatFilterService;
    private MessageConfig messageConfig;
    private NetworkRedisService NRS;
    BanList banList;
    private Tasks tasks;
    private WinterCoreApi api;
    private NameMcService nameMcService;
    private PacketSender packetSender;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        isShuttingDown = false;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        loadRanksFile();

        instance = this;
        this.tasks = new Tasks(this);
        this.packetSender = new PacketSender(getLogger());
        this.messageConfig = new MessageConfig(this);
        this.chatFilterService = new ChatFilterService(this);
        this.nameMcService = new NameMcService(this);

        this.databaseManager = DatabaseManager.init(this);

        initializeRedis();
        initializePlaceholders();

        RankManager.initialize(this);
        this.rankManager = RankManager.getInstance();
        this.rankManager.startAutoCacheRefresh();
        this.permissionConfigManager = new PermissionConfigManager(this);

        tasks.timerAsync(
                () -> databaseManager.getModerationService().removeExpiredBans(),
                0L,
                20L
        );

        this.staffModeManager = new StaffModeManager(this);
        this.disguiseRegistry = new DisguiseRegistry(this.redisManager, getLogger());
        this.disguiseHandler = new DefaultDisguiseHandler(this, this.disguiseRegistry);
        this.tagsManager = new TagsManager(this);
        this.tagsMenu = new TagsMenu(this, tagsManager);
        this.menuConfig = new MenuConfig(this);
        this.disguiseEventListener = new DisguiseEventListener(this, (DefaultDisguiseHandler) disguiseHandler);
        this.commandHandler = new CommandHandler(this);

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
        isShuttingDown = true;
        getLogger().info("Disabling WinterCore, publishing offline status...");

        if (disguiseEventListener != null) {
            disguiseEventListener.clearDisguiseOnShutdown();
        }

        if (disguiseRegistry != null) {
            disguiseRegistry.shutdown();
        }

        if (redisManager != null) {
            try {
                redisManager.stop();
                redisManager.publish(new ServerStatusPacket(
                        getConfig().getString("server-name", "Unknown"),
                        System.currentTimeMillis(),
                        false
                ));
            } catch (Exception e) {
                getLogger().warning("Failed to publish offline status: " + e.getMessage());
            }
        }

        if (redisPool != null) {
            try (Jedis jedis = redisPool.getResource()) {
                for (Player online : Utilities.getOnlinePlayers()) {
                    if (NRS != null) {
                        NRS.clearOnlinePresence(online.getUniqueId(), online.getName());
                    }
                }
                jedis.del("server:" + getConfig().getString("server-name", "unknown") + ":heartbeat");
                jedis.del("server:" + getConfig().getString("server-name", "unknown") + ":info");
            } catch (Exception e) {
                getLogger().warning("Failed to cleanup Redis keys on disable: " + e.getMessage());
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        if (redisPool != null) {
            redisPool.close();
        }

        saveVanishedPlayers();

        if (nameTagColorManager != null) {
            nameTagColorManager.unload();
        }

        if (api != null) {
            getServer().getServicesManager().unregister(WinterCoreApi.class, api);
            api = null;
        }

        getLogger().info(CC.translate("&cWinterCore has been disabled."));
        instance = null;
    }
    private void initializeRedis() {
        String redisHost = getConfig().getString("Redis.host", "localhost");
        int redisPort = getConfig().getInt("Redis.port", 6379);
        String redisPassword = getConfig().getString("Redis.password", "");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        this.redisPool = redisPassword.isEmpty()
                ? new JedisPool(poolConfig, redisHost, redisPort)
                : new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);

        this.redisManager = new RedisManager(this);
        this.redisSocials = new RedisSocials(this);
        this.socialInput = new SocialInput(this, this.redisSocials);
        this.NRS = new NetworkRedisService(this);

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
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", (ch, player, message) -> { });
    }

    private void saveVanishedPlayers() {
        getConfig().set("vanished_players", vanishedPlayers.stream().map(UUID::toString).collect(Collectors.toList()));
        saveConfig();
    }

    private void loadRanksFile() {
        if (!new File(getDataFolder(), "ranks.yml").exists()) {
            saveResource("ranks.yml", false);
        }
    }

    public NetworkRedisService getNRS() {
        if (NRS == null) {
            throw new IllegalStateException("NetworkRedisService not initialized");
        }
        return NRS;
    }
}
