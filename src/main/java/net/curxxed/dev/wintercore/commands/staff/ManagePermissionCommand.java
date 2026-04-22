package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.permissions.PermissionConfigManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "permission",
        permission = {"wintercore.managepermissions", "wintercore.admin", "wintercore.manager"},
        description = "Manage persistent permission overrides for a player.",
        usage = "/permission <player> <add|deny|remove|list> [permission]",
        inGameOnly = false
)
public class ManagePermissionCommand extends BaseCommand {

    public ManagePermissionCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() < 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: " + commandInfo.usage());
            return;
        }

        Player targetPlayer = plugin.getServer().getPlayer(commandArgs.getArgs()[0]);
        if (targetPlayer == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase(Locale.ENGLISH);
        PermissionConfigManager manager = plugin.getPermissionConfigManager();

        if ("list".equals(action)) {
            PermissionConfigManager.PermissionEntrySnapshot snapshot =
                    manager.getSnapshot(targetPlayer.getUniqueId());
            List<String> grants = snapshot.getGrants();
            List<String> denies = snapshot.getDenies();

            commandArgs.getSender().sendMessage(ChatColor.AQUA + "Permission overrides for " + targetPlayer.getName() + ":");
            commandArgs.getSender().sendMessage(ChatColor.GRAY + "  Grants: " +
                    (grants.isEmpty() ? ChatColor.DARK_GRAY + "(none)" : ChatColor.GREEN + String.join(", ", grants)));
            commandArgs.getSender().sendMessage(ChatColor.GRAY + "  Denies: " +
                    (denies.isEmpty() ? ChatColor.DARK_GRAY + "(none)" : ChatColor.RED + String.join(", ", denies)));
            return;
        }

        if (commandArgs.length() < 3) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: " + commandInfo.usage());
            return;
        }

        String permissionNode = normalizePermission(commandArgs.getArgs()[2]);
        if (permissionNode.isEmpty()) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Permission node cannot be empty.");
            return;
        }

        if ("add".equals(action)) {
            manager.setGranted(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Granted " + permissionNode + " to " + targetPlayer.getName() + ".");
            return;
        }

        if ("deny".equals(action)) {
            manager.setDenied(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Denied " + permissionNode + " for " + targetPlayer.getName() + ".");
            return;
        }

        if ("remove".equals(action)) {
            manager.removeOverride(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Removed override " + permissionNode + " from " + targetPlayer.getName() + ".");
            return;
        }

        commandArgs.getSender().sendMessage(ChatColor.RED + "Invalid action. Use add, deny, remove, or list.");
    }

    private void refreshPlayerPermissions(Player player) {
        plugin.getRankManager().refreshPlayerDisplay(player);
        plugin.getRankManager().refreshPlayerDisplayForAll(player);
        plugin.getPlayerService().syncUpdate(player.getUniqueId());
    }

    private String normalizePermission(String node) {
        if (node == null) {
            return "";
        }
        String normalized = node.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
