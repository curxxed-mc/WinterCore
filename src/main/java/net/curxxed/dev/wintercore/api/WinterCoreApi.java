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
import net.curxxed.dev.wintercore.database.service.IdentityService;
import net.curxxed.dev.wintercore.database.service.ModerationService;
import net.curxxed.dev.wintercore.database.service.ProfileService;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.namemc.NameMcService;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.nms.PacketSender;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.player.WinterCorePlayer;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.scheduler.Tasks;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.staff.VanishService;
import net.curxxed.dev.wintercore.tags.TagsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WinterCoreApi {

    int API_VERSION = 2;

    /** Returns the registered API, or an empty value while WinterCore is unavailable. */
    static Optional<WinterCoreApi> find() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(WinterCoreApi.class));
    }

    /** Returns the registered API or fails clearly when WinterCore is not enabled. */
    static WinterCoreApi get() {
        return find().orElseThrow(() -> new IllegalStateException("WinterCore API is not available"));
    }

    default int apiVersion() {
        return API_VERSION;
    }

    String pluginVersion();

    String serverName();

    boolean isEnabled();

    boolean isShuttingDown();

    Tasks tasks();

    PacketSender packetSender();

    String serverVersion();

    boolean isAtLeastMinecraft(int minorVersion);

    CurrencyService currency();

    default ProfileService profiles() {
        return database().getProfileService();
    }

    default IdentityService identities() {
        return database().getIdentityService();
    }

    default ModerationService moderation() {
        return database().getModerationService();
    }

    DatabaseManager database();

    RedisManager redis();

    NetworkRedisService network();

    RedisSocials socials();

    MessageConfig messages();

    ChatFilterService chatFilter();

    MessagingService messaging();

    StaffChatService staffChat();

    RankManager ranks();

    TagsManager tags();

    PlayerService players();

    StaffModeManager staffMode();

    VanishService vanish();

    FreezeListener freeze();

    DisguiseHandler disguises();

    DisguiseRegistry disguiseRegistry();

    NameMcService nameMc();

    NameTagColorManager nametags();

    AuthManager authentication();

    PermissionConfigManager permissions();

    MenuConfig menus();

    List<Player> onlinePlayers();

    void publish(RedisPacket<?> packet);

    default Optional<Player> onlinePlayer(UUID uuid) {
        return Optional.ofNullable(Bukkit.getPlayer(Objects.requireNonNull(uuid, "uuid")));
    }

    default Optional<WinterCorePlayer> player(UUID uuid) {
        return Optional.ofNullable(players().getPlayerData(Objects.requireNonNull(uuid, "uuid")));
    }

    default Optional<DisguiseData> disguise(UUID uuid) {
        return Optional.ofNullable(disguiseRegistry().getDisguiseData(Objects.requireNonNull(uuid, "uuid")));
    }

    default boolean isDisguised(UUID uuid) {
        return disguiseRegistry().isDisguised(Objects.requireNonNull(uuid, "uuid"));
    }

    default boolean isVanished(UUID uuid) {
        return vanish().isVanished(Objects.requireNonNull(uuid, "uuid"));
    }

    default boolean isFrozen(UUID uuid) {
        return freeze().isFrozen(Objects.requireNonNull(uuid, "uuid"));
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<Long> getBalance(UUID uuid) {
        CompletableFuture<Long> result = new CompletableFuture<>();
        currency().getBalance(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<String> getRank(UUID uuid) {
        CompletableFuture<String> result = new CompletableFuture<>();
        profiles().getRank(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<String> getTag(UUID uuid) {
        CompletableFuture<String> result = new CompletableFuture<>();
        profiles().getPlayerTag(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<String> getChatColor(UUID uuid) {
        CompletableFuture<String> result = new CompletableFuture<>();
        profiles().getChatColorPreference(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<String> getPlayerName(UUID uuid) {
        CompletableFuture<String> result = new CompletableFuture<>();
        identities().getPlayerName(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. A missing identity completes with {@code null}. */
    default CompletableFuture<UUID> getPlayerId(String name) {
        CompletableFuture<UUID> result = new CompletableFuture<>();
        identities().getUUIDByName(Objects.requireNonNull(name, "name"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. */
    default CompletableFuture<Set<UUID>> getAlternateAccounts(UUID uuid) {
        CompletableFuture<Set<UUID>> result = new CompletableFuture<>();
        identities().getAlts(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }

    /** Completes on the Bukkit main thread. No active ban is represented by {@code null}. */
    default CompletableFuture<ModerationService.ActiveBan> getActiveBan(UUID uuid) {
        CompletableFuture<ModerationService.ActiveBan> result = new CompletableFuture<>();
        moderation().getActiveBan(Objects.requireNonNull(uuid, "uuid"), result::complete);
        return result;
    }
}
