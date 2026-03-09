package net.curxxed.dev.wintercore.plugin;

import lombok.Getter;
import lombok.var;
import net.curxxed.dev.wintercore.auth.AuthModule;
import net.curxxed.dev.wintercore.client.ClientBrand;
import net.curxxed.dev.wintercore.client.ClientBrandCommand;
import net.curxxed.dev.wintercore.commands.api.BrigadierCommandHandler;
import net.curxxed.dev.wintercore.commands.api.CommandHandler;
import net.curxxed.dev.wintercore.commands.bungee.ServerManagerCommand;
import net.curxxed.dev.wintercore.commands.bungee.SyncCommand;
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
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.commands.DisguiseCommand;
import net.curxxed.dev.wintercore.disguise.commands.UnDisguiseCommand;
import net.curxxed.dev.wintercore.disguise.impl.DefaultDisguiseHandler;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.listeners.ChatListener;
import net.curxxed.dev.wintercore.listeners.ConnectionListener;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.listeners.PlayerListener;
import net.curxxed.dev.wintercore.menu.MenuManager;
import net.curxxed.dev.wintercore.menus.MenuConfig;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.placeholders.Placeholder;
import net.curxxed.dev.wintercore.rank.RankCommand;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.staff.StaffModeListener;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.tags.TagsCommand;
import net.curxxed.dev.wintercore.tags.TagsGUI;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.BanList;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.util.*;

@Getter
public final class WinterCore extends JavaPlugin {

    @Getter
    private static WinterCore instance;
    public static volatile boolean isShuttingDown = false;

    private boolean placeholderAPIEnabled = false;
    private final Map<UUID, DisguiseData> disguiseDataMap = new HashMap<>();
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    private DatabaseManager databaseManager;
    private RankManager rankManager;
    private JedisPool redisPool;
    private RedisManager redisManager;
    private Placeholder placeholder;
    private TagsManager tagsManager;
    private TagsGUI tagsGUI;
    private DisguiseRegistry disguiseRegistry;
    private DisguiseEventListener disguiseEventListener;
    private PlayerListener playerListener;
    private ChatListener chatListener;
    private FreezeListener freezeListener;
    private StaffModeManager staffModeManager;
    private DisguiseHandler disguiseHandler;
    private CommandHandler commandHandler;
    private AuthModule authModule;
    private MenuConfig menuConfig;
    private NameTagColorManager nameTagColorManager;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        loadRanksFile();

        instance = this;

        this.databaseManager = DatabaseManager.init(this);

        initializeRedis();
        initializePlaceholders();

        RankManager.initialize(this);
        this.rankManager = RankManager.getInstance();
        this.rankManager.startAutoCacheRefresh();

        getServer().getScheduler().runTaskTimerAsynchronously(
                this, () -> databaseManager.removeExpiredBans(), 0L, 20L);

        this.staffModeManager      = new StaffModeManager(this);
        this.disguiseRegistry      = new DisguiseRegistry(this.redisManager, getLogger());
        this.disguiseHandler       = new DefaultDisguiseHandler(this, this.disguiseRegistry);
        this.tagsManager           = new TagsManager(this);
        this.tagsGUI               = new TagsGUI(tagsManager);
        this.menuConfig            = new MenuConfig(this);
        this.disguiseEventListener = new DisguiseEventListener(this, (DefaultDisguiseHandler) disguiseHandler);
        this.commandHandler        = new CommandHandler(this);

        MenuManager.initialize(this);
        registerListeners();
        registerCommands();
        registerBungee();

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

        if (redisManager != null) {
            try {
                redisManager.publishServerStatus(false);
            } catch (Exception e) {
                getLogger().warning("Failed to publish offline status: " + e.getMessage());
            }
        }

        if (redisPool != null) {
            try (var jedis = redisPool.getResource()) {
                jedis.del("server:" + getConfig().getString("server-name", "unknown") + ":heartbeat");
                for (String key : jedis.keys("disguise:*")) jedis.del(key);
            } catch (Exception e) {
                getLogger().warning("Failed to cleanup Redis keys on disable: " + e.getMessage());
            }
        }

        if (databaseManager != null) databaseManager.close();
        if (redisPool != null) redisPool.close();

        saveVanishedPlayers();

        if (nameTagColorManager != null) nameTagColorManager.unload();

        getLogger().info(CC.translate("&cWinterCore has been disabled."));
        instance = null;
    }

    private void initializeRedis() {
        String redisHost     = getConfig().getString("Redis.host", "localhost");
        int    redisPort     = getConfig().getInt("Redis.port", 6379);
        String redisPassword = getConfig().getString("Redis.password", "");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        try (JedisPool tempPool = buildJedisPool(poolConfig, redisHost, redisPort, redisPassword); var jedis = tempPool.getResource()) {
            if (jedis.keys("server:*").isEmpty()) {
                for (String key : jedis.keys("disguise:*")) jedis.del(key);
                getLogger().info("No server:* keys found — stale disguise:* keys cleared.");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to cleanup stale disguise keys on startup: " + e.getMessage());
        }

        this.redisPool    = buildJedisPool(poolConfig, redisHost, redisPort, redisPassword);
        this.redisManager = new RedisManager(this);

        redisManager.startHeartbeatSender();
        redisManager.startHeartbeatMonitor();
        redisManager.publishServerStatus(true);
        redisManager.startInfoUpdater();

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
        PluginManager pm = getServer().getPluginManager();

        this.playerListener = new PlayerListener(this);
        this.chatListener   = new ChatListener(this, tagsManager, playerListener);
        this.freezeListener = new FreezeListener(playerListener);

        pm.registerEvents(playerListener, this);
        pm.registerEvents(chatListener, this);
        pm.registerEvents(new ConnectionListener(this), this);
        pm.registerEvents(freezeListener, this);

        pm.registerEvents(new RankMenu.ChatListener(this), this);

        pm.registerEvents(new SocialInput(this), this);
        pm.registerEvents(new StaffModeListener(this, staffModeManager), this);
        pm.registerEvents(tagsGUI, this);
        pm.registerEvents(disguiseEventListener, this);
        pm.registerEvents(new BanList(this), this);
    }

    private void registerCommands() {
        commandHandler.register(new FreezeCommand(freezeListener, this));
        commandHandler.register(ThruCommand.class);
        commandHandler.register(Fly.class);
        commandHandler.register(new TrollCommand(this));
        commandHandler.register(InvSeeCommand.class);
        commandHandler.register(Feed.class);
        commandHandler.register(ClearChat.class);
        commandHandler.register(new ChatColorCommand(this));
        commandHandler.register(new GameModeCommand(this, staffModeManager));
        commandHandler.register(DiscordCommand.class);
        commandHandler.register(Heal.class);
        commandHandler.register(new GrantCommand(this));
        commandHandler.register(ManagePermissionCommand.class);
        commandHandler.register(ReloadConfig.class);
        commandHandler.register(new ListCommand(this, rankManager));
        commandHandler.register(VanishCommand.class);
        commandHandler.register(new ReportCommand(this, tagsManager));
        commandHandler.register(new StaffChatCommand(this, chatListener));
        commandHandler.register(new AdminChatCommand(this, chatListener));
        commandHandler.register(new ManagerChatCommand(this, chatListener));
        commandHandler.register(AboutCommand.class);
        commandHandler.register(MuteCommand.class);
        commandHandler.register(KickCommand.class);
        commandHandler.register(BanCommand.class);
        commandHandler.register(WarningCommand.class);
        commandHandler.register(UnmuteCommand.class);
        commandHandler.register(new HistoryCommand(this, menuConfig));
        commandHandler.register(FixCommand.class);
        commandHandler.register(MoreCommand.class);
        commandHandler.register(EnchantCommand.class);
        commandHandler.register(PingCommand.class);
        commandHandler.register(MessageCommand.class);
        commandHandler.register(SpeedCommand.class);
        commandHandler.register(ClearEffectsCommand.class);

        ProfileCommand profile = new ProfileCommand(this, redisManager);
        getServer().getPluginManager().registerEvents(profile, this);
        commandHandler.register(profile);

        commandHandler.register(ServerManagerCommand.class);
        commandHandler.register(JumpToPlayer.class);
        commandHandler.register(StaffListCommand.class);
        commandHandler.register(new StaffModeCommand(this, staffModeManager));
        commandHandler.register(new RankCommand(this, rankManager));
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
        commandHandler.register(SyncCommand.class);

        this.authModule = new AuthModule(this, databaseManager.getDatabase());
        this.authModule.register(commandHandler);
    }

    private void registerBungee() {
        String channel = Utilities.IS_1_13_OR_NEWER ?  "minecraft:brand" : "MC|Brand";
        Bukkit.getMessenger().registerIncomingPluginChannel(this, channel, new ClientBrand(this));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, channel);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", (ch, player, message) -> {});
    }

    public void saveVanishedPlayers() {
        Set<String> uuids = new HashSet<>();
        for (UUID uuid : vanishedPlayers) uuids.add(uuid.toString());
        getConfig().set("vanished_players", uuids);
        saveConfig();
    }

    public void loadRanksFile() {
        File ranksFile = new File(getDataFolder(), "ranks.yml");
        if (!ranksFile.exists()) saveResource("ranks.yml", false);
        YamlConfiguration.loadConfiguration(ranksFile);
        getLogger().info("Ranks file loaded.");
    }
}