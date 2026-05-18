package net.curxxed.dev.wintercore.plugin;

import lombok.Getter;
import net.curxxed.dev.wintercore.api.SimpleWinterCoreApi;
import net.curxxed.dev.wintercore.api.WinterCoreApi;
import net.curxxed.dev.wintercore.auth.AuthModule;
import net.curxxed.dev.wintercore.chat.ChatFilterService;
import net.curxxed.dev.wintercore.chat.ChatListener;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.client.ClientBrand;
import net.curxxed.dev.wintercore.commands.staff.ClientBrandCommand;
import net.curxxed.dev.wintercore.commands.framework.BrigadierCommandHandler;
import net.curxxed.dev.wintercore.commands.framework.CommandHandler;
import net.curxxed.dev.wintercore.commands.bungee.ServerManagerCommand;
import net.curxxed.dev.wintercore.commands.gamemode.GameModeCommand;
import net.curxxed.dev.wintercore.commands.misc.*;
import net.curxxed.dev.wintercore.commands.network.*;
import net.curxxed.dev.wintercore.commands.social.DiscordCommand;
import net.curxxed.dev.wintercore.commands.staff.*;
import net.curxxed.dev.wintercore.commands.troll.TrollCommand;
import net.curxxed.dev.wintercore.commands.utility.*;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.RedisSocials;
import net.curxxed.dev.wintercore.database.redis.SocialInput;
import net.curxxed.dev.wintercore.database.redis.packet.packets.ServerStatusPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.commands.DisguiseCommand;
import net.curxxed.dev.wintercore.disguise.commands.UnDisguiseCommand;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.listeners.ConnectionListener;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.menu.MenuManager;
import net.curxxed.dev.wintercore.config.MenuConfig;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.namemc.NameMcService;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.nms.PacketSender;
import net.curxxed.dev.wintercore.config.PermissionConfigManager;
import net.curxxed.dev.wintercore.placeholders.Placeholder;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.rank.RankCommand;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.scheduler.Tasks;
import net.curxxed.dev.wintercore.staff.StaffModeListener;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.commands.social.TagsCommand;
import net.curxxed.dev.wintercore.menus.TagsMenu;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.player.BanList;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.config.MessageConfig;
import net.curxxed.dev.wintercore.utils.Utilities;
import net.curxxed.dev.wintercore.version.PlayerProtocolResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public final class WinterCore extends JavaPlugin {

    @Getter
    private static WinterCore instance;

    public static volatile boolean isShuttingDown = false;
    public String channel = Utilities.IS_1_13_OR_NEWER ? "minecraft:brand" : "MC|Brand";

    private boolean placeholderAPIEnabled = false;
    private final ConcurrentMap<UUID, DisguiseData> disguiseDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

    private DatabaseManager databaseManager;
    private RankManager rankManager;
    private JedisPool redisPool;
    private RedisManager redisManager;
    private RedisSocials redisSocials;
    private SocialInput socialInput;
    private Placeholder placeholder;
    private TagsManager tagsManager;
    private TagsMenu tagsMenu;
    private DisguiseRegistry disguiseRegistry;
    private DisguiseEventListener disguiseEventListener;
    private PlayerService playerService;
    private MessagingService messagingService;
    private ChatListener chatListener;
    private FreezeListener freezeListener;
    private StaffModeManager staffModeManager;
    private DisguiseHandler disguiseHandler;
    private CommandHandler commandHandler;
    private AuthModule authModule;
    private MenuConfig menuConfig;
    private PermissionConfigManager permissionConfigManager;
    private NameTagColorManager nameTagColorManager;
    private StaffChatService staffChatService;
    private ChatFilterService chatFilterService;
    private MessageConfig messageConfig;
    private NetworkRedisService NRS;
    private BanList banList;
    private Tasks tasks;
    private WinterCoreApi api;
    private NameMcService nameMcService;
    private PacketSender packetSender;
    private PlayerProtocolResolver protocolResolver;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        loadRanksFile();

        instance = this;
        this.tasks = new Tasks(this);
        this.packetSender = new PacketSender(getLogger());
        this.protocolResolver = new PlayerProtocolResolver(this);
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
        registerListeners();
        registerCommands();
        registerBungee();
        registerApi();

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
        Utilities.logBootBanner();
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
                for (Player online : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
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

        unregisterApi();

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

        this.redisPool = buildJedisPool(poolConfig, redisHost, redisPort, redisPassword);

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

        for (Player player : Utilities.getOnlinePlayers()) {
            ClientBrandCommand.silenced.add(player.getUniqueId());
        }
    }

    private JedisPool buildJedisPool(JedisPoolConfig config, String host, int port, String password) {
        return password.isEmpty()
                ? new JedisPool(config, host, port)
                : new JedisPool(config, host, port, 2000, password);
    }

    private void initializePlaceholders() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("PlaceholderAPI not found, some features may not work.");
            return;
        }

        getLogger().info("PlaceholderAPI found, initializing placeholders.");
        this.placeholder = new Placeholder(this);

        if (this.placeholder.register()) {
            placeholderAPIEnabled = true;
            getLogger().info("WinterCore placeholders registered.");
        } else {
            getLogger().warning("Failed to register WinterCore placeholders.");
        }
    }

    private void registerListeners() {
        WinterCoreListeners listeners = new WinterCoreListenerRegistrar(this).register();
        this.playerService = listeners.getPlayerService();
        this.messagingService = listeners.getMessagingService();
        this.staffChatService = listeners.getStaffChatService();
        this.chatListener = listeners.getChatListener();
        this.freezeListener = listeners.getFreezeListener();
        this.banList = listeners.getBanList();
    }

    private void registerCommands() {
        this.authModule = new WinterCoreCommandRegistrar(this).register();
    }

    private void registerBungee() {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, channel, new ClientBrand(this));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, channel);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", (ch, player, message) -> { });
    }

    private void registerApi() {
        this.api = new SimpleWinterCoreApi(this);
        getServer().getServicesManager().register(WinterCoreApi.class, api, this, ServicePriority.Normal);
    }

    private void unregisterApi() {
        if (api != null) {
            getServer().getServicesManager().unregister(WinterCoreApi.class, api);
            api = null;
        }
    }

    public void saveVanishedPlayers() {
        Set<String> uuids = new HashSet<>();
        for (UUID uuid : vanishedPlayers) {
            uuids.add(uuid.toString());
        }
        getConfig().set("vanished_players", uuids);
        saveConfig();
    }

    public void loadRanksFile() {
        File ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) {
            saveResource("ranks.yml", false);
        }
        YamlConfiguration.loadConfiguration(ranksFile);
        getLogger().info("Ranks file loaded.");
    }

    public NetworkRedisService getNRS() {
        if (NRS == null) {
            throw new IllegalStateException("NetworkRedisService not initialized");
        }
        return NRS;
    }
}
