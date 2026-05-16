package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "2fa",
        description = "Manage your two-factor authentication setup.",
        usage = "/2fa <setup|disable> [code]",
        inGameOnly = true,
        async = true,
        permission = {"wintercore.staff.2fa"}
)
public class TwoFACommand extends BaseCommand {

    private final AuthManager authManager;

    public TwoFACommand(WinterCore plugin, AuthManager authManager) {
        super(plugin);
        this.authManager = authManager;
    }

    @Override
    public void execute(CommandArguments args) {
        runSync(() -> executeOnMainThread(args));
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            return completeCurrentArg(args, Arrays.asList("setup", "disable"));
        }

        return Collections.emptyList();
    }

    private void executeOnMainThread(CommandArguments args) {
        Player player = args.getPlayer();

        if (args.length() == 0) {
            sendUsage(player);
            return;
        }

        String mode = args.getArgs()[0].toLowerCase(Locale.ENGLISH);
        if ("setup".equals(mode)) {
            handleSetup(player);
            return;
        }
        if ("disable".equals(mode)) {
            handleDisable(player, args);
            return;
        }

        sendUsage(player);
    }

    private void handleSetup(Player player) {
        authManager.hasSecretConfiguredAsync(player.getUniqueId(), configured -> runSync(() -> {
            if (!player.isOnline()) {
                return;
            }

            if (configured) {
                send(player, "auth.already-configured", "&c2FA is already configured on your account.");
                send(player, "auth.disable-first", "&7Run &e/2fa disable <code> &7to reset it first.");
                return;
            }

            runAsync(() -> {
                AuthManager.SetupResult result = authManager.generateAndSaveSecret(player);
                runSync(() -> {
                    sendList(player, "auth.setup-details", Arrays.asList(
                            "",
                            "&6&l2FA Setup",
                            "&7Scan the QR code or enter the key manually into a TOTP app.",
                            "",
                            "&eSecret Key: &f{secret}",
                            "",
                            "&bOTP URL (paste into a QR generator):",
                            "&f{otp_url}",
                            "",
                            "&cSave your secret key in a safe place.",
                            ""
                    ), "{secret}", result.secret,
                            "{otp_url}", result.otpUrl);

                    authManager.completeSetup(player);
                });
            });
        }));
    }

    private void handleDisable(Player player, CommandArguments args) {
        authManager.hasSecretConfiguredAsync(player.getUniqueId(), configured -> runSync(() -> {
            if (!player.isOnline()) {
                return;
            }

            if (!configured) {
                send(player, "auth.not-configured", "&c2FA is not configured on your account.");
                return;
            }

            if (args.length() < 2) {
                send(player, "auth.confirm-required", "&cYou must confirm with your current code: &e/2fa disable <code>");
                return;
            }

            final int code;
            try {
                code = Integer.parseInt(args.getArgs()[1].trim());
            } catch (NumberFormatException e) {
                send(player, "auth.invalid-code", "&cInvalid code. Enter your 6-digit authenticator code.");
                return;
            }

            authManager.authenticateAsync(player, code, success -> runSync(() -> {
                if (!player.isOnline()) {
                    return;
                }

                if (success) {
                    authManager.disableAuth(player);
                    send(player, "auth.disabled", "&a2FA has been disabled on your account.");
                } else {
                    send(player, "auth.disable-failed", "&cInvalid code. 2FA was not disabled.");
                }
            }));
        }));
    }

    private void sendUsage(Player player) {
        sendList(player, "auth.help", Arrays.asList(
                "",
                "&6&l2FA Commands",
                "&e/2fa setup &7- Set up 2FA for your account",
                "&e/2fa disable <code> &7- Remove 2FA (requires current code)",
                ""
        ));
    }
}
