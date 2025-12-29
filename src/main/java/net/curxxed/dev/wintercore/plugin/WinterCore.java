package net.curxxed.dev.wintercore.plugin;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.var;
import net.curxxed.dev.wintercore.commands.api.CommandHandler;
import net.curxxed.dev.wintercore.nametags.NameTagHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import net.curxxed.dev.wintercore.client.*;
import net.curxxed.dev.wintercore.commands.gamemode.*;
import net.curxxed.dev.wintercore.commands.bungee.*;
import net.curxxed.dev.wintercore.commands.misc.*;
import net.curxxed.dev.wintercore.commands.social.*;
import net.curxxed.dev.wintercore.commands.troll.*;
import net.curxxed.dev.wintercore.commands.staff.*;
import net.curxxed.dev.wintercore.commands.utility.*;
import net.curxxed.dev.wintercore.rank.*;
import net.curxxed.dev.wintercore.tags.*;
import net.curxxed.dev.wintercore.disguise.*;
import net.curxxed.dev.wintercore.disguise.impl.*;
import net.curxxed.dev.wintercore.database.*;
import net.curxxed.dev.wintercore.listeners.*;
import net.curxxed.dev.wintercore.menus.*;
import net.curxxed.dev.wintercore.staff.*;
import net.curxxed.dev.wintercore.disguise.commands.*;
import net.curxxed.dev.wintercore.disguise.player.*;
import net.curxxed.dev.wintercore.placeholders.*;
import net.curxxed.dev.wintercore.utils.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

@Getter
@Setter
public final class WinterCore extends JavaPlugin {

    public static WinterCore INSTANCE;
    public static volatile boolean isShuttingDown = false;
    private boolean SpawnHubDetected = false;
    private boolean NotCurxxedSpawnHubDetected = false;
    private boolean placeholderAPIEnabled = false;
    private final Map<UUID, DisguiseData> disguiseDataMap = new HashMap<>();
    public Set<UUID> vanished_players = new HashSet<>();
    private RankManager rankManager;
    private ChatColorSelectionMenu chatColorSelectionMenu;
    private JedisPool redisPool;
    private RedisManager redisManager;
    private FreezeCommand freezeCommand;
    public DatabaseManager databaseManager;
    private Plugin plugin;
    private Gson gson;
    private Placeholder placeholder;
    private DisguiseData disguiseData;
    private TagsManager tagsManager;
    private TagsGUI tagsGUI;
    private boolean winterSpigotDetected = false;
    private DisguiseRegistry disguiseRegistry;
    private NameTagHandler nameTagHandler;
    private DisguiseEventListener disguiseEventListener;
    private PlayerListener playerListener = new PlayerListener(this, tagsManager);
    private FreezeListener freezeListener = new FreezeListener(playerListener);
    private StaffModeManager staffModeManager;
    private ReachListener reachListener;
    private RankChangeEvent rankChangeEvent;
    private DisguiseHandler disguiseHandler;
    private RankMenu rankMenu;
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        loadRanksFile();
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        INSTANCE = this;
        try {
            Class.forName("net.curxxed.dev.WinterSpigot");
            winterSpigotDetected = true;
            getLogger().info("WinterSpigot detected!");
        } catch (ClassNotFoundException e) {
            winterSpigotDetected = false;
        }

        initializePlaceholders();

        this.databaseManager = DatabaseManager.init(this);

        this.rankManager = new RankManager(this);
        this.chatColorSelectionMenu = new ChatColorSelectionMenu(this);
        String redisHost = getConfig().getString("Redis.host", "localhost");
        int redisPort = getConfig().getInt("Redis.port", 6379);
        String redisPassword = getConfig().getString("Redis.password", "");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Utilities.log("&bPlaceholderAPI successfully registered.");
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        JedisPool tempPool;
        if (redisPassword.isEmpty()) {
            tempPool = new JedisPool(poolConfig, redisHost, redisPort);
        } else {
            tempPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
        }
        try (var jedis = tempPool.getResource()) {
            boolean hasServerKey = !jedis.keys("server:*").isEmpty();
            if (!hasServerKey) {
                Set<String> disguiseKeys = jedis.keys("disguise:*");
                for (String key : disguiseKeys) {
                    jedis.del(key);
                }
                getLogger().info("No server:* keys found in Redis. All disguise:* keys have been deleted for crash/force-kill cleanup.");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to check or cleanup disguise keys on startup: " + e.getMessage());
        }
        tempPool.close();

        if (redisPassword.isEmpty()) {
            this.redisPool = new JedisPool(poolConfig, redisHost, redisPort);
        } else {
            this.redisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword);
        }
        this.redisManager = new RedisManager(this);
        for (Player player : Utilities.getOnlinePlayers()) {
            ClientBrandCommand.silenced.add(player.getUniqueId());
        }

        redisManager.startHeartbeatSender();
        redisManager.startHeartbeatMonitor();
        redisManager.publishServerStatus(true);
        redisManager.startInfoUpdater();
        rankManager.startAutoCacheRefresh();
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> databaseManager.removeExpiredBans(), 0L, 20L);
        getLogger().info("WinterCore has been enabled in " + (System.currentTimeMillis() - start) + "ms!");
        Utilities.logBootBanner();
        RankManager.initialize(this);
        this.rankManager = RankManager.getInstance();
        this.staffModeManager = new StaffModeManager(this);
        this.disguiseRegistry = new DisguiseRegistry(this.redisManager, getLogger());
        this.disguiseHandler = new DefaultDisguiseHandler(this, this.disguiseRegistry);
        this.tagsManager = new TagsManager(this);
        this.tagsGUI = new TagsGUI(tagsManager);
        this.disguiseEventListener = new DisguiseEventListener(this, (DefaultDisguiseHandler) disguiseHandler);

        // Initialize command handler before registering commands
        this.commandHandler = new CommandHandler(this);

        registerListeners();
        registerCommands();
        registerBungee();
        this.nameTagHandler = new NameTagHandler(this);
        this.nameTagHandler.load();
    }

    private void registerBungee() {
        Messenger bm = getServer().getMessenger();
        bm.registerOutgoingPluginChannel(this, "BungeeCord");
        bm.registerIncomingPluginChannel(this, "minecraft:brand", new ClientBrand(this));
        bm.registerIncomingPluginChannel(this, "MC|Brand", new ClientBrand(this));
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        playerListener = new PlayerListener(this, tagsManager);
        freezeListener = new FreezeListener(playerListener);
        SocialInput socialInput = new SocialInput(this);
        pm.registerEvents(playerListener, this);
        rankMenu = new RankMenu(this);
        pm.registerEvents(rankMenu, this);
        pm.registerEvents(freezeListener, this);
        pm.registerEvents(new ProfileCommand(this, getRedisManager()), this);
        pm.registerEvents(socialInput, this);
        pm.registerEvents(new StaffListMenu(this), this);
        pm.registerEvents(new StaffModeListener(this, staffModeManager), this);
        pm.registerEvents(new ChatColorSelectionMenu(this), this);
        pm.registerEvents(new DisguiseGUI(disguiseHandler), this);
        pm.registerEvents(tagsGUI, this);
        pm.registerEvents(this.disguiseEventListener, this);
        pm.registerEvents(new HistoryCommand(this), this);
        pm.registerEvents(new BanList(this), this);
    }

    private void registerCommands() {
        // Commands that require additional constructor args or implement Listener are registered as instances
        commandHandler.register(new FreezeCommand(freezeListener, this));
        commandHandler.register(ThruCommand.class);
        commandHandler.register(Fly.class);
        commandHandler.register(new TrollCommand(this, reachListener));
        commandHandler.register(InvSeeCommand.class);
        commandHandler.register(Feed.class);
        commandHandler.register(ClearChat.class);
        // ChatColorSelectionMenu implements Listener - create single instance and register for both events and command
        ChatColorSelectionMenu chatColor = new ChatColorSelectionMenu(this);
        getServer().getPluginManager().registerEvents(chatColor, this);
        commandHandler.register(chatColor);
        commandHandler.register(new GameModeCommand(this, staffModeManager));
        commandHandler.register(DiscordCommand.class);
        commandHandler.register(Heal.class);
        commandHandler.register(GrantCommand.class);
        commandHandler.register(ManagePermissionCommand.class);
        commandHandler.register(ReloadConfig.class);
        commandHandler.register(new ListCommand(this, rankManager));
        commandHandler.register(VanishCommand.class);
        commandHandler.register(new ReportCommand(this, tagsManager));
        commandHandler.register(new StaffChatCommand(this, playerListener));
        commandHandler.register(new AdminChatCommand(this, playerListener));
        commandHandler.register(new ManagerChatCommand(this, playerListener));
        commandHandler.register(AboutCommand.class);
        commandHandler.register(MuteCommand.class);
        commandHandler.register(KickCommand.class);
        commandHandler.register(BanCommand.class);
        commandHandler.register(WarningCommand.class);
        commandHandler.register(UnmuteCommand.class);
        // HistoryCommand implements Listener - reuse single instance
        HistoryCommand history = new HistoryCommand(this);
        getServer().getPluginManager().registerEvents(history, this);
        commandHandler.register(history);
        commandHandler.register(FixCommand.class);
        commandHandler.register(MoreCommand.class);
        commandHandler.register(EnchantCommand.class);
        commandHandler.register(PingCommand.class);
        commandHandler.register(MessageCommand.class);
        commandHandler.register(SpeedCommand.class);
        commandHandler.register(ClearEffectsCommand.class);
        // ProfileCommand implements Listener & requires RedisManager - reuse single instance
        ProfileCommand profile = new ProfileCommand(this, getRedisManager());
        getServer().getPluginManager().registerEvents(profile, this);
        commandHandler.register(profile);
        commandHandler.register(ServerManagerCommand.class);
        commandHandler.register(JumpToPlayer.class);
        commandHandler.register(StaffListCommand.class);
        commandHandler.register(new StaffModeCommand(this, staffModeManager));
        commandHandler.register(new RankCommand(this, RankManager.getInstance()));
        commandHandler.register(CheckNMS.class);
        commandHandler.register(SudoCommand.class);
        commandHandler.register(UnbanCommand.class);
        commandHandler.register(ClientBrandCommand.class);
        commandHandler.register(new TagsCommand(tagsGUI, this));
        commandHandler.register(new DisguiseCommand(disguiseHandler, this));
        commandHandler.register(new UnDisguiseCommand(disguiseHandler, this));
        commandHandler.register(ReplyCommand.class);
        commandHandler.register(AltsCommand.class);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling server, publishing offline status");
        if (disguiseRegistry != null && redisManager != null) {
            try {
                Field disguisedPlayersField = disguiseRegistry.getClass().getDeclaredField("disguisedPlayers");
                disguisedPlayersField.setAccessible(true);
                Set<UUID> disguisedPlayers = (Set<UUID>) disguisedPlayersField.get(disguiseRegistry);
                for (UUID uuid : disguisedPlayers) {
                    redisManager.clearDisguise(uuid);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to clear disguise strings from Redis: " + e.getMessage());
            }
        }
        disguiseEventListener.clearDisguiseOnShutdown();
        if (redisManager != null) {
            try {
                redisManager.publishServerStatus(false);
            } catch (Exception e) {
                getLogger().warning("RedisManager was not initialized or failed, could not publish offline status.");
            }
        }

        if (redisPool != null) {
            try (var jedis = redisPool.getResource()) {
                String key = "server:" + getConfig().getString("server-name", "unknown") + ":heartbeat";
                jedis.del(key);
            } catch (Exception e) {
                getLogger().warning("Failed to remove heartbeat key: " + e.getMessage());
            }
        }

        if (redisPool != null) {
            try (var jedis = redisPool.getResource()) {
                Set<String> keys = jedis.keys("disguise:*");
                for (String key : keys) {
                    jedis.del(key);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to remove all disguise:* keys: " + e.getMessage());
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
        if (redisPool != null) {
            redisPool.close();
        }
        if (rankManager != null) {
            rankManager.closeDatabaseConnection();
        }
        saveVanishedPlayers();
        if (nameTagHandler != null) {
            nameTagHandler.clear();
        }
        getLogger().info("§cWinterCore has been disabled.");
        INSTANCE = null;
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

    private void initializePlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found, initializing placeholders.");
            placeholder = new Placeholder(this);
            placeholderAPIEnabled = true;
        } else {
            placeholder = null;
            placeholderAPIEnabled = false;
            getLogger().warning("PlaceholderAPI not found, some features may not work.");
        }
    }
}
