package net.curxxed.dev.wintercore.api;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.chat.ChatFilterService;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.config.MenuConfig;
import net.curxxed.dev.wintercore.config.MessageConfig;
import net.curxxed.dev.wintercore.config.PermissionConfigManager;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.RedisSocials;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import net.curxxed.dev.wintercore.database.redis.service.NetworkRedisService;
import net.curxxed.dev.wintercore.database.service.CurrencyService;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.namemc.NameMcService;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.nms.PacketSender;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.scheduler.Tasks;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.staff.VanishService;
import net.curxxed.dev.wintercore.tags.TagsManager;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.util.List;

public final class SimpleWinterCoreApi implements WinterCoreApi {

    private final WinterCore plugin;

    public SimpleWinterCoreApi(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String pluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String serverName() {
        return plugin.getConfig().getString("server-name", "Unknown");
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public boolean isShuttingDown() {
        return WinterCore.isShuttingDown();
    }

    @Override
    public Tasks tasks() {
        return plugin.getTasks();
    }

    @Override
    public PacketSender packetSender() {
        return plugin.getPacketSender();
    }


    @Override
    public String serverVersion() {
        return Utilities.getServerVersion();
    }

    @Override
    public boolean isAtLeastMinecraft(int minorVersion) {
        return Utilities.getMinecraftMinorVersion() >= minorVersion;
    }

    @Override
    public CurrencyService currency() {
        return plugin.getDatabaseManager().getCurrencyService();
    }

    @Override
    public DatabaseManager database() {
        return plugin.getDatabaseManager();
    }

    @Override
    public RedisManager redis() {
        return plugin.getRedisManager();
    }

    @Override
    public NetworkRedisService network() {
        return plugin.getNetworkRedisService();
    }

    @Override
    public RedisSocials socials() {
        return plugin.getRedisSocials();
    }

    @Override
    public MessageConfig messages() {
        return plugin.getMessageConfig();
    }

    @Override
    public ChatFilterService chatFilter() {
        return plugin.getChatFilterService();
    }

    @Override
    public MessagingService messaging() {
        return plugin.getMessagingService();
    }

    @Override
    public StaffChatService staffChat() {
        return plugin.getStaffChatService();
    }

    @Override
    public RankManager ranks() {
        return plugin.getRankManager();
    }

    @Override
    public TagsManager tags() {
        return plugin.getTagsManager();
    }

    @Override
    public PlayerService players() {
        return plugin.getPlayerService();
    }

    @Override
    public StaffModeManager staffMode() {
        return plugin.getStaffModeManager();
    }

    @Override
    public VanishService vanish() {
        return plugin.getVanishService();
    }

    @Override
    public FreezeListener freeze() {
        return plugin.getFreezeListener();
    }

    @Override
    public DisguiseHandler disguises() {
        return plugin.getDisguiseHandler();
    }

    @Override
    public DisguiseRegistry disguiseRegistry() {
        return plugin.getDisguiseRegistry();
    }

    @Override
    public NameMcService nameMc() {
        return plugin.getNameMcService();
    }

    @Override
    public NameTagColorManager nametags() {
        return plugin.getNameTagColorManager();
    }

    @Override
    public AuthManager authentication() {
        return plugin.getAuthModule().getAuthManager();
    }

    @Override
    public PermissionConfigManager permissions() {
        return plugin.getPermissionConfigManager();
    }

    @Override
    public MenuConfig menus() {
        return plugin.getMenuConfig();
    }

    @Override
    public List<Player> onlinePlayers() {
        return Utilities.getOnlinePlayers();
    }

    @Override
    public void publish(RedisPacket<?> packet) {
        plugin.getRedisManager().publish(packet);
    }
}
