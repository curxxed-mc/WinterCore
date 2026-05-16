package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.Arrays;

@CommandInfo(
        name = "auth",
        description = "Authenticate with your 2FA code after joining.",
        usage = "/auth <6-digit code>",
        inGameOnly = true,
        async = true,
        permission = {}
)
public class AuthCommand extends BaseCommand {

    private final AuthManager authManager;

    public AuthCommand(WinterCore plugin, AuthManager authManager) {
        super(plugin);
        this.authManager = authManager;
    }

    @Override
    public void execute(CommandArguments args) {
        runSync(() -> executeOnMainThread(args));
    }

    private void executeOnMainThread(CommandArguments args) {
        final Player player = args.getPlayer();

        if (!player.hasPermission(AuthManager.STAFF_PERMISSION)) {
            send(player, "auth.no-setup-required", "&c2FA is reserved for staff members.");
            return;
        }

        if (args.length() == 0) {
            sendUsage(args.getSender());
            return;
        }

        final int code;
        try {
            code = Integer.parseInt(args.getArgs()[0].trim());
        } catch (NumberFormatException e) {
            send(player, "auth.invalid-code", "&cInvalid code. Enter the 6-digit code from your authenticator app.");
            return;
        }

        authManager.hasSecretConfiguredAsync(player.getUniqueId(), hasSecret -> runSync(() -> {
            if (!player.isOnline()) {
                return;
            }

            if (!hasSecret) {
                send(player, "auth.setup-required", "&c&l2FA Setup Required");
                return;
            }

            if (authManager.isAuthenticated(player)) {
                send(player, "auth.authenticated", "&a&lAuthenticated &8(session resumed)");
                return;
            }

            authManager.authenticateAsync(player, code, success -> runSync(() -> {
                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    sendList(player, "auth.success", Arrays.asList(
                            "",
                            "&a&lAuthenticated &8(session resumed)",
                            "&7Session valid for &e12 hours &7or until your IP changes.",
                            ""
                    ));
                } else {
                    send(player, "auth.invalid-code", "&cInvalid code. Enter the 6-digit code from your authenticator app.");
                }
            }));
        }));
    }
}
