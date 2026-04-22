package net.curxxed.dev.wintercore.auth;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import net.curxxed.dev.wintercore.auth.repository.AuthRepository;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AuthManager {

    public static final String STAFF_PERMISSION = "wintercore.staff.2fa";

    private static final long AUTH_TIMEOUT_TICKS = 30 * 20L;
    private static final long SESSION_DURATION_MS = 12 * 60 * 60 * 1000L;
    private static final String TOTP_ISSUER = "WinterCore";

    private final WinterCore plugin;
    private final AuthRepository repository;
    private final GoogleAuthenticator googleAuth;
    private final JedisPool jedisPool;

    private final Map<UUID, BukkitTask> pendingAuth = new ConcurrentHashMap<>();
    private final Set<UUID> pendingSetup = ConcurrentHashMap.newKeySet();

    public AuthManager(WinterCore plugin, AuthRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.googleAuth = new GoogleAuthenticator();
        this.jedisPool = plugin.getRedisPool();
    }

    public void handleJoin(Player player) {
        if (!isStaff(player)) return;

        final UUID uuid = player.getUniqueId();
        final String currentIp = resolveIp(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean hasSecret = repository.hasSecret(uuid);
            AuthSession session = hasSecret ? getSession(uuid) : null;
            boolean resumed = session != null && session.isValid(currentIp);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline()) {
                    return;
                }

                if (!hasSecret) {
                    pendingSetup.add(uuid);
                    online.sendMessage("");
                    online.sendMessage(CC.translate("&c&l2FA Setup Required"));
                    online.sendMessage(CC.translate("&7 2FA is mandatory for staff."));
                    online.sendMessage(CC.translate("&7 Use &e/2fa setup &7to get started."));
                    online.sendMessage(CC.translate("&7 You have &e30 seconds&7 or you will be kicked."));
                    online.sendMessage("");
                    scheduleKickTimer(online, CC.translate("&c&lSetup Timeout\n\n&72FA setup is mandatory for staff.\n&7Please reconnect and run /2fa setup."));
                    return;
                }

                if (resumed) {
                    online.sendMessage(CC.translate("&a&lAuthenticated &8(session resumed)"));
                    return;
                }

                scheduleKickTimer(online, CC.translate("&c&lAuthentication Timeout\n\n&7You did not authenticate within 30 seconds.\n&7Please reconnect and enter your 2FA code."));

                online.sendMessage("");
                online.sendMessage(CC.translate("&c&lAuthentication Required"));
                online.sendMessage(CC.translate("&7 You have &e30 seconds &7to verify your identity."));
                online.sendMessage(CC.translate("&7 Use &e/auth <6-digit code> &7from your authenticator app."));
                online.sendMessage("");
            });
        });
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelKickTimer(uuid);
        pendingSetup.remove(uuid);
    }

    public boolean authenticate(Player player, int totpCode) {
        UUID uuid = player.getUniqueId();
        boolean success = authenticate(uuid, resolveIp(player), totpCode);
        if (success) {
            cancelKickTimer(uuid);
        }
        return success;
    }

    public void authenticateAsync(Player player, int totpCode, Consumer<Boolean> callback) {
        UUID uuid = player.getUniqueId();
        String currentIp = resolveIp(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = authenticate(uuid, currentIp, totpCode);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    cancelKickTimer(uuid);
                }
                callback.accept(success);
            });
        });
    }

    public void hasSecretConfiguredAsync(UUID uuid, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean configured = repository.hasSecret(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(configured));
        });
    }

    public SetupResult generateAndSaveSecret(Player player) {
        GoogleAuthenticatorKey credentials = googleAuth.createCredentials();
        String secret = credentials.getKey();
        repository.saveSecret(player.getUniqueId(), secret);

        String otpUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                TOTP_ISSUER, player.getName(), credentials
        );

        return new SetupResult(secret, otpUrl);
    }

    public void completeSetup(Player player) {
        UUID uuid = player.getUniqueId();
        pendingSetup.remove(uuid);
        cancelKickTimer(uuid);

        scheduleKickTimer(player, CC.translate("&c&lAuthentication Timeout\n\n&7You did not authenticate within 30 seconds.\n&7Please reconnect and enter your 2FA code."));

        player.sendMessage("");
        player.sendMessage(CC.translate("&a2FA configured. Now authenticate to continue."));
        player.sendMessage(CC.translate("&7 Use &e/auth <6-digit code> &7from your authenticator app."));
        player.sendMessage(CC.translate("&7 You have &e30 seconds&7."));
        player.sendMessage("");
    }

    public void disableAuth(Player player) {
        UUID uuid = player.getUniqueId();
        repository.deleteSecret(uuid);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(sessionKey(uuid));
        }
        cancelKickTimer(uuid);
    }

    public boolean isStaff(Player player) {
        return player.hasPermission(STAFF_PERMISSION);
    }

    public boolean requiresAuth(Player player) {
        return isStaff(player) && repository.hasSecret(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        if (!isStaff(player)) return true;

        AuthSession session = getSession(player.getUniqueId());
        return session != null && session.isValid(resolveIp(player));
    }

    public boolean isPendingAuth(Player player) {
        return pendingAuth.containsKey(player.getUniqueId());
    }

    public boolean isPendingSetup(Player player) {
        return pendingSetup.contains(player.getUniqueId());
    }

    public boolean hasSecretConfigured(UUID uuid) {
        return repository.hasSecret(uuid);
    }

    private void scheduleKickTimer(Player player, String kickMessage) {
        UUID uuid = player.getUniqueId();
        cancelKickTimer(uuid);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingAuth.remove(uuid);
            pendingSetup.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                online.kickPlayer(kickMessage);
            }
        }, AUTH_TIMEOUT_TICKS);

        pendingAuth.put(uuid, task);
    }

    private void cancelKickTimer(UUID uuid) {
        BukkitTask task = pendingAuth.remove(uuid);
        if (task != null) task.cancel();
    }

    private String resolveIp(Player player) {
        if (player == null || player.getAddress() == null || player.getAddress().getAddress() == null) {
            return "";
        }
        return player.getAddress().getAddress().getHostAddress();
    }

    private boolean authenticate(UUID uuid, String currentIp, int totpCode) {
        String secret = repository.getSecret(uuid).join();
        if (secret == null) return false;
        if (!googleAuth.authorize(secret, totpCode)) return false;

        long expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS;
        saveSession(new AuthSession(uuid, currentIp, expiresAt));
        return true;
    }

    private void saveSession(AuthSession session) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = sessionKey(session.getPlayerUUID());
            jedis.hset(key, "ip", session.getIpAddress());
            jedis.hset(key, "expiry", String.valueOf(session.getExpiresAt()));
            jedis.expireAt(key, session.getExpiresAt() / 1000L);
        }
    }

    private AuthSession getSession(UUID uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = sessionKey(uuid);
            String ip = jedis.hget(key, "ip");
            String expiryValue = jedis.hget(key, "expiry");
            if (ip == null || expiryValue == null) {
                return null;
            }

            long expiresAt;
            try {
                expiresAt = Long.parseLong(expiryValue);
            } catch (NumberFormatException ignored) {
                return null;
            }

            return new AuthSession(uuid, ip, expiresAt);
        }
    }

    private String sessionKey(UUID uuid) {
        return "auth:session:" + uuid;
    }

    public static class SetupResult {
        public final String secret;
        public final String otpUrl;

        public SetupResult(String secret, String otpUrl) {
            this.secret = secret;
            this.otpUrl = otpUrl;
        }
    }
}
