/**
 * @authors curxxed, samm, FaceSlap_
 * @since iCore (WinterCore) 0.01
 * @revision 0.9
 *
 * <p>
 *     WinterCore is a comprehensive Minecraft server management plugin that provides a wide range of features for server administrators and staff members.
 *     It includes functionalities such as player disguises, rank management, chat color selection, staff mode, and integration with Redis for cross-server communication.
 * </p>
 *
 * <p>
 *     It also includes support for PlaceholderAPI to provide dynamic placeholders for ranks, disguises, and other player-related information.
 * </p>
 *
 * <p>
 *     WinterCore is built with performance and reliability in mind, utilizing asynchronous tasks for database operations and Redis communication to minimize server lag.
 *     It also includes robust error handling and logging to assist with troubleshooting and maintenance.
 * </p>
 *
 *
 */

package net.curxxed.dev.wintercore.plugin;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.var;
import net.curxxed.dev.wintercore.client.ClientBrand;
import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.commands.api.CommandHandler;
import net.curxxed.dev.wintercore.commands.bungee.ServerManagerCommand;
import net.curxxed.dev.wintercore.commands.gamemode.GameModeCommand;
import net.curxxed.dev.wintercore.commands.misc.*;
import net.curxxed.dev.wintercore.commands.social.DiscordCommand;
import net.curxxed.dev.wintercore.commands.staff.*;
import net.curxxed.dev.wintercore.commands.troll.TrollCommand;
import net.curxxed.dev.wintercore.commands.utility.*;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.database.RedisManager;
import net.curxxed.dev.wintercore.database.SocialInput;
import net.curxxed.dev.wintercore.disguise.DisguiseEventListener;
import net.curxxed.dev.wintercore.disguise.DisguiseGUI;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.commands.DisguiseCommand;
import net.curxxed.dev.wintercore.disguise.commands.UnDisguiseCommand;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import net.curxxed.dev.wintercore.listeners.ReachListener;
import net.curxxed.dev.wintercore.menus.ChatColorSelectionMenu;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.menus.StaffListMenu;
import net.curxxed.dev.wintercore.nametags.NameTagHandler;
import net.curxxed.dev.wintercore.placeholders.Placeholder;
import net.curxxed.dev.wintercore.rank.RankChangeEvent;
import net.curxxed.dev.wintercore.rank.RankCommand;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.staff.StaffModeListener;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.tags.TagsCommand;
import net.curxxed.dev.wintercore.tags.TagsGUI;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.BanList;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
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

    public void registerBungee() {
        String channel;
        if (Utilities.IS_LEGACY) {
            channel = "MC|Brand";
        } else {
            channel = "minecraft:brand";
        }

        Bukkit.getMessenger().registerIncomingPluginChannel(this, channel, new ClientBrand(this));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, channel);
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
        commandHandler.register(WhoIsDisguisedCommand.class);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling server, publishing offline status");
        if (disguiseRegistry != null && redisManager != null) {
            try {
                Set<UUID> disguisedPlayers = new HashSet<>(disguiseRegistry.disguisedPlayers);
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
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI found, initializing placeholders.");

            this.placeholder = new Placeholder(this);

            if (this.placeholder.register()) {
                placeholderAPIEnabled = true;
                getLogger().info("WinterCore placeholders registered.");
            } else {
                placeholderAPIEnabled = false;
                getLogger().warning("Failed to register WinterCore placeholders.");
            }
        } else {
            placeholder = null;
            placeholderAPIEnabled = false;
            getLogger().warning("PlaceholderAPI not found, some features may not work.");
        }
    }

}
