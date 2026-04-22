package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "auth",
        description = "Authenticate with your 2FA code after joining.",
        usage = "/auth <6-digit code>",
        inGameOnly = true
)
public class AuthCommand extends BaseCommand {

    private final AuthManager authManager;

    public AuthCommand(WinterCore plugin, AuthManager authManager) {
        super(plugin);
        this.authManager = authManager;
    }

    @Override
    public void execute(CommandArguments args) {
        final Player player = args.getPlayer();

        if (!player.hasPermission(AuthManager.STAFF_PERMISSION)) {
            player.sendMessage(CC.translate("&c2FA is reserved for staff members."));
            return;
        }

        if (args.length() == 0) {
            player.sendMessage(CC.translate("&cUsage: &e/auth <6-digit code>"));
            return;
        }

        final int code;
        try {
            code = Integer.parseInt(args.getArgs()[0].trim());
        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&cInvalid code. Enter the 6-digit code from your authenticator app."));
            return;
        }

        authManager.hasSecretConfiguredAsync(player.getUniqueId(), hasSecret -> {
            if (!hasSecret) {
                player.sendMessage(CC.translate("&cYou have not set up 2FA yet. Use &e/2fa setup &cto begin."));
                return;
            }

            if (authManager.isAuthenticated(player)) {
                player.sendMessage(CC.translate("&aYou are already authenticated."));
                return;
            }

            authManager.authenticateAsync(player, code, success -> {
                if (success) {
                    player.sendMessage("");
                    player.sendMessage(CC.translate("&a&lAuthenticated successfully."));
                    player.sendMessage(CC.translate("&7Session valid for &e12 hours &7or until your IP changes."));
                    player.sendMessage("");
                } else {
                    player.sendMessage(CC.translate("&cInvalid code. Please check your authenticator app and try again."));
                }
            });
        });
    }
}
