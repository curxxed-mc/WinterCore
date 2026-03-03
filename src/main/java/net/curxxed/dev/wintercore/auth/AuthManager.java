package net.curxxed.dev.wintercore.auth;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import net.curxxed.dev.wintercore.auth.repository.AuthRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    public static final String STAFF_PERMISSION = "wintercore.staff.2fa";

    private static final long AUTH_TIMEOUT_TICKS = 30 * 20L;
    private static final long SESSION_DURATION_MS = 12 * 60 * 60 * 1000L;
    private static final String TOTP_ISSUER = "WinterCore";

    private final JavaPlugin plugin;
    private final AuthRepository repository;
    private final GoogleAuthenticator googleAuth;

    private final Map<UUID, BukkitTask> pendingAuth = new ConcurrentHashMap<>();
    private final Set<UUID> pendingSetup = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthManager(JavaPlugin plugin, AuthRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.googleAuth = new GoogleAuthenticator();
    }

    public void handleJoin(Player player) {
        if (!isStaff(player)) return;

        UUID uuid = player.getUniqueId();

        if (!repository.hasSecret(uuid)) {
            pendingSetup.add(uuid);
            player.sendMessage("");
            player.sendMessage("§c§l  ⚠ 2FA Setup Required");
            player.sendMessage("§7  2FA is mandatory for staff. You must configure it before proceeding.");
            player.sendMessage("§7  Use §e/2fa setup §7to get started.");
            player.sendMessage("§7  You have §e30 seconds §7or you will be kicked.");
            player.sendMessage("");
            scheduleKickTimer(player, "§c§lSetup Timeout\n\n§72FA setup is mandatory for staff.\n§7Please reconnect and run /2fa setup.");
            return;
        }

        AuthSession existing = sessions.get(uuid);
        if (existing != null && existing.isValid(resolveIp(player))) {
            player.sendMessage("§a§lAuthenticated §8(session resumed)");
            return;
        }

        sessions.remove(uuid);
        scheduleKickTimer(player, "§c§lAuthentication Timeout\n\n§7You did not authenticate within 30 seconds.\n§7Please reconnect and enter your 2FA code.");

        player.sendMessage("");
        player.sendMessage("§c§l  ⚠ Authentication Required");
        player.sendMessage("§7  You have §e30 seconds §7to verify your identity.");
        player.sendMessage("§7  Use §e/auth <6-digit code> §7from your authenticator app.");
        player.sendMessage("");
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelKickTimer(uuid);
        pendingSetup.remove(uuid);
    }

    public boolean authenticate(Player player, int totpCode) {
        UUID uuid = player.getUniqueId();
        String secret = repository.getSecret(uuid);

        if (secret == null) return false;

        if (googleAuth.authorize(secret, totpCode)) {
            long expiry = System.currentTimeMillis() + SESSION_DURATION_MS;
            sessions.put(uuid, new AuthSession(uuid, resolveIp(player), expiry));
            cancelKickTimer(uuid);
            return true;
        }
        return false;
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

        scheduleKickTimer(player, "§c§lAuthentication Timeout\n\n§7You did not authenticate within 30 seconds.\n§7Please reconnect and enter your 2FA code.");

        player.sendMessage("");
        player.sendMessage("§a  2FA configured! Now authenticate to continue.");
        player.sendMessage("§7  Use §e/auth <6-digit code> §7from your authenticator app.");
        player.sendMessage("§7  You have §e30 seconds§7.");
        player.sendMessage("");
    }

    public void disableAuth(Player player) {
        UUID uuid = player.getUniqueId();
        repository.deleteSecret(uuid);
        sessions.remove(uuid);
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
        if (!repository.hasSecret(player.getUniqueId())) return false;

        AuthSession session = sessions.get(player.getUniqueId());
        if (session == null) return false;

        if (!session.isValid(resolveIp(player))) {
            sessions.remove(player.getUniqueId());
            return false;
        }
        return true;
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
        return player.getAddress().getAddress().getHostAddress();
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