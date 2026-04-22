package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "2fa",
        description = "Manage your two-factor authentication setup.",
        usage = "/2fa <setup|disable> [code]",
        permission = "wintercore.staff.2fa",
        inGameOnly = true
)
public class TwoFACommand extends BaseCommand {

    private final AuthManager authManager;

    public TwoFACommand(WinterCore plugin, AuthManager authManager) {
        super(plugin);
        this.authManager = authManager;
    }

    @Override
    public void execute(CommandArguments args) {
        Player player = args.getPlayer();

        if (args.length() == 0) {
            sendUsage(player);
            return;
        }

        String mode = args.getArgs()[0].toLowerCase();
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
        authManager.hasSecretConfiguredAsync(player.getUniqueId(), configured -> {
            if (configured) {
                player.sendMessage(CC.translate("&c2FA is already configured on your account."));
                player.sendMessage(CC.translate("&7Run &e/2fa disable <code> &7to reset it first."));
                return;
            }

            AuthManager.SetupResult result = authManager.generateAndSaveSecret(player);

            player.sendMessage("");
            player.sendMessage(CC.translate("&6&l2FA Setup"));
            player.sendMessage(CC.translate("&7Scan the QR code or enter the key manually into a TOTP app."));
            player.sendMessage("");
            player.sendMessage(CC.translate("&eSecret Key: &f" + result.secret));
            player.sendMessage("");
            player.sendMessage(CC.translate("&bOTP URL (paste into a QR generator):"));
            player.sendMessage(CC.translate("&f" + result.otpUrl));
            player.sendMessage("");
            player.sendMessage(CC.translate("&cSave your secret key in a safe place."));
            player.sendMessage("");

            authManager.completeSetup(player);
        });
    }

    private void handleDisable(Player player, CommandArguments args) {
        authManager.hasSecretConfiguredAsync(player.getUniqueId(), configured -> {
            if (!configured) {
                player.sendMessage(CC.translate("&c2FA is not configured on your account."));
                return;
            }

            if (args.length() < 2) {
                player.sendMessage(CC.translate("&cYou must confirm with your current code: &e/2fa disable <code>"));
                return;
            }

            final int code;
            try {
                code = Integer.parseInt(args.getArgs()[1].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(CC.translate("&cInvalid code. Enter your 6-digit authenticator code."));
                return;
            }

            authManager.authenticateAsync(player, code, success -> {
                if (success) {
                    authManager.disableAuth(player);
                    player.sendMessage(CC.translate("&a2FA has been disabled on your account."));
                } else {
                    player.sendMessage(CC.translate("&cInvalid code. 2FA was not disabled."));
                }
            });
        });
    }

    private void sendUsage(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.translate("&6&l2FA Commands"));
        player.sendMessage(CC.translate("&e/2fa setup &7- Set up 2FA for your account"));
        player.sendMessage(CC.translate("&e/2fa disable <code> &7- Remove 2FA (requires current code)"));
        player.sendMessage("");
    }
}
