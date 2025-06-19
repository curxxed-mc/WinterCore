package net.curxxed.dev.icore.plugin;

import com.google.gson.Gson;
import net.curxxed.dev.CommandAPI.CommandManager;
import net.curxxed.dev.icore.commands.staff.FreezeCommand;
import net.curxxed.dev.icore.database.DatabaseManager;
import net.curxxed.dev.icore.database.RedisManager;
import net.curxxed.dev.icore.disguise.commands.DisguiseCommand;
import net.curxxed.dev.icore.disguise.commands.UnDisguiseCommand;
import net.curxxed.dev.icore.nametags.DefaultNameTagAdapter;
import net.curxxed.dev.icore.nametags.NameTagAdapter;
import net.curxxed.dev.icore.placeholders.Placeholder;
import net.curxxed.dev.icore.gui.ColorGUI;
import net.curxxed.dev.icore.tags.TagsGUI;
import net.curxxed.dev.icore.tags.TagsManager;
import net.curxxed.dev.icore.disguise.player.DisguiseData;
import net.curxxed.dev.icore.listeners.PlayerListener;
import net.curxxed.dev.icore.rank.RankManager;
import net.curxxed.dev.icore.utils.NMSUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.var;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;
import net.curxxed.dev.icore.client.*;
import net.curxxed.dev.icore.commands.gamemode.*;
import net.curxxed.dev.icore.commands.bungee.*;
import net.curxxed.dev.icore.commands.misc.*;
import net.curxxed.dev.icore.commands.social.*;
import net.curxxed.dev.icore.commands.troll.*;
import net.curxxed.dev.icore.commands.staff.*;
import net.curxxed.dev.icore.commands.utility.*;
import net.curxxed.dev.icore.rank.*;
import net.curxxed.dev.icore.tags.*;
import net.curxxed.dev.icore.disguise.*;
import net.curxxed.dev.icore.disguise.impl.*;
import net.curxxed.dev.icore.database.*;
import net.curxxed.dev.icore.listeners.*;
import net.curxxed.dev.icore.gui.*;
import net.curxxed.dev.icore.staff.*;
import net.curxxed.dev.icore.disguise.DisguiseRegistry;

import java.io.File;
import java.util.*;

public final class iCore extends JavaPlugin {

    public static iCore INSTANCE;

    @Getter
    private boolean placeholderAPIEnabled = false;
    @Getter
    private final Map<UUID, DisguiseData> disguiseDataMap = new HashMap<>();
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
    @Setter
    private Plugin plugin;
    @Setter
    @Getter
    private Gson gson;
    @Getter
    private NameTagAdapter nameTagAdapter;
    @Getter
    private Placeholder placeholder;
    @Getter
    private DisguiseData disguiseData;
    @Getter
    @Setter
    private TagsManager tagsManager;
    @Setter
    @Getter
    private TagsGUI tagsGUI;
    @Getter
    private boolean winterSpigotDetected = false;
    @Getter
    private DisguiseRegistry disguiseRegistry;

    private PlayerListener playerListener;
    private FreezeListener freezeListener;
    private StaffModeManager staffModeManager;
    private ReachListener reachListener;
    private RankChangeEvent rankChangeEvent;
    @Getter
    @Setter
    private DisguiseHandler disguiseHandler;

    @Override
    public void onEnable() {
        new CommandManager(this);
        this.nameTagAdapter = new DefaultNameTagAdapter();
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
        rankManager.startAutoCacheRefresh();
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> databaseManager.removeExpiredBans(), 0L, 20L);
        getLogger().info("iCore has been enabled in " + (System.currentTimeMillis() - start) + "ms!");
        NMSUtils.logBootBanner();

        // --- RegisterPlugin logic moved here ---
        RankManager.initialize(this);
        this.rankManager = RankManager.getInstance();
        this.staffModeManager = new StaffModeManager(this);
        this.disguiseRegistry = new DisguiseRegistry(this.redisManager, getLogger());
        this.disguiseHandler = new DefaultDisguiseHandler(this, this.disguiseRegistry);
        this.tagsManager = new TagsManager(this);
        this.tagsGUI = new TagsGUI(tagsManager);
        registerListeners();
        registerCommands();
        registerBungee();
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
        pm.registerEvents(new RankGUIListener(this), this);
        pm.registerEvents(freezeListener, this);
        pm.registerEvents(new ProfileCommand(this, getRedisManager()), this);
        pm.registerEvents(socialInput, this);
        pm.registerEvents(new StaffListGUI(this), this);
        pm.registerEvents(new StaffModeListener(this, staffModeManager), this);
        pm.registerEvents(new ColorGUI(this), this);
        pm.registerEvents(new DisguiseGUI(disguiseHandler), this);
        pm.registerEvents(tagsGUI, this);
        // Register DisguiseEventListener
        pm.registerEvents(new DisguiseEventListener(this, (DefaultDisguiseHandler) disguiseHandler), this);
    }

    private void registerCommands() {
        new CommandManager(this);
        if (freezeListener == null) {
            freezeListener = new FreezeListener(playerListener);
        }
        new FreezeCommand(freezeListener);
        new ThruCommand();
        new Fly();
        new TrollCommand(this, reachListener);
        new InvSeeCommand();
        new Feed();
        new ClearChat();
        new ColorGUI(this);
        new gmc(staffModeManager);
        new gma(staffModeManager);
        new gms(staffModeManager);
        new gmsp(staffModeManager);
        new DiscordCommand();
        new Heal();
        new SetRankCommand(this);
        new ManagePermissionCommand(this);
        new ReloadConfig(this);
        new ListCommand(this, rankManager);
        new VanishCommand(this);
        new ReportCommand(this, tagsManager);
        new StaffChatCommand(this, playerListener);
        new AdminChatCommand(this, playerListener);
        new ManagerChatCommand(this, playerListener);
        new AboutCommand(this);
        new MuteCommand(this);
        new KickCommand(this);
        new BanCommand(this);
        new WarnCommand(this);
        new UnmuteCommand(this);
        new HistoryCommand(this);
        new FixCommand(this);
        new MoreCommand(this);
        new EnchantCommand();
        new PingCommand();
        new MessageCommand(this);
        new SpeedCommand();
        new ClearEffectsCommand();
        new ProfileCommand(this, getRedisManager());
        new ServerManagerCommand(this);
        new JumpToPlayer();
        new StaffListCommand(this);
        new StaffModeCommand(this, staffModeManager);
        new idontknowwhatthisisdontlook();
        new RankCommand(this, RankManager.getInstance());
        new CheckNMS();
        new SudoCommand();
        new UnbanCommand(this);
        new ClientBrandCommand(this);
        new TagsCommand(tagsGUI);
        new DisguiseCommand(disguiseHandler);
        new UnDisguiseCommand(disguiseHandler);
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
        if (disguiseRegistry != null && disguiseHandler != null) {
            disguiseRegistry.clearAll(player -> {
                try {
                    disguiseHandler.unDisguise(player, true);
                } catch (Exception e) {
                    getLogger().warning("Failed to undisguise player on shutdown: " + player.getName());
                }
            });
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (redisPool != null) {
            redisPool.close();
        }
        rankManager.closeDatabaseConnection();
        saveVanishedPlayers();
        getLogger().info("§ciCore has been disabled.");
        INSTANCE = null;
    }

    public PlayerListener getPlayerListener() {
        return new PlayerListener(this, tagsManager);
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
