package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.auth.AuthManager;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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
        Player player = args.getPlayer();

        if (!player.hasPermission(AuthManager.STAFF_PERMISSION)) {
            player.sendMessage("§c2FA is reserved for staff members.");
            return;
        }

        if (!authManager.hasSecretConfigured(player.getUniqueId())) {
            player.sendMessage("§cYou haven't set up 2FA yet. Use §e/2fa setup §cto get started.");
            return;
        }

        if (authManager.isAuthenticated(player)) {
            player.sendMessage("§aYou are already authenticated.");
            return;
        }

        if (args.length() == 0) {
            player.sendMessage("§cUsage: §e/auth <6-digit code>");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(args.getArgs()[0].trim());
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid code — please enter the 6-digit number from your authenticator app.");
            return;
        }

        if (authManager.authenticate(player, code)) {
            player.sendMessage("");
            player.sendMessage("§a§l  ✔ Authenticated successfully!");
            player.sendMessage("§7  Session valid for §e12 hours §7or until your IP changes.");
            player.sendMessage("");
        } else {
            player.sendMessage("§c✘ Invalid code. Please check your authenticator app and try again.");
        }
    }
}
