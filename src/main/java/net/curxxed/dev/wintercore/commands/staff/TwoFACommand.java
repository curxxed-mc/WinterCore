package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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

        switch (args.getArgs()[0].toLowerCase()) {
            case "setup":
                handleSetup(player);
                break;
            case "disable":
                handleDisable(player, args);
                break;
            default:
                sendUsage(player);
                break;
        }
    }

    private void handleSetup(Player player) {
        if (authManager.hasSecretConfigured(player.getUniqueId())) {
            player.sendMessage("§c2FA is already configured on your account.");
            player.sendMessage("§7Run §e/2fa disable <code> §7to reset it first.");
            return;
        }

        AuthManager.SetupResult result = authManager.generateAndSaveSecret(player);

        player.sendMessage("");
        player.sendMessage("§6§l  2FA Setup");
        player.sendMessage("§7  Scan the QR code or enter the key manually into");
        player.sendMessage("§7  Google Authenticator, Authy, or any TOTP app.");
        player.sendMessage("");
        player.sendMessage("§e  Secret Key: §f" + result.secret);
        player.sendMessage("");
        player.sendMessage("§b  OTP URL (paste into a QR generator):");
        player.sendMessage("§f  " + result.otpUrl);
        player.sendMessage("");
        player.sendMessage("§c§l  ⚠ Save your secret key somewhere safe!");
        player.sendMessage("");

        authManager.completeSetup(player);
    }

    private void handleDisable(Player player, CommandArguments args) {
        if (!authManager.hasSecretConfigured(player.getUniqueId())) {
            player.sendMessage("§c2FA is not configured on your account.");
            return;
        }

        if (args.length() < 2) {
            player.sendMessage("§cYou must confirm with your current code: §e/2fa disable <code>");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(args.getArgs()[1].trim());
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid code — please enter your 6-digit authenticator code.");
            return;
        }

        if (authManager.authenticate(player, code)) {
            authManager.disableAuth(player);
            player.sendMessage("§a2FA has been disabled on your account.");
        } else {
            player.sendMessage("§c✘ Invalid code — 2FA was NOT disabled.");
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l  2FA Commands");
        player.sendMessage("§e  /2fa setup            §7— Set up 2FA for your account");
        player.sendMessage("§e  /2fa disable <code>   §7— Remove 2FA (requires current code)");
        player.sendMessage("");
    }
}