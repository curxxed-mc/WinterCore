package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.permissions.PermissionConfigManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@CommandInfo(
        name = "permission",
        description = "Manage persistent permission overrides for a player.",
        usage = "/permission <player> <add|deny|remove|list> [permission]",
        inGameOnly = false,
        permission = {"wintercore.managepermissions", "wintercore.admin", "wintercore.manager"}
)
public class ManagePermissionCommand extends BaseCommand {

    private static final List<String> ACTIONS = java.util.Arrays.asList("add", "deny", "remove", "list");

    public ManagePermissionCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() < 2) {
            commandArgs.getSender().sendMessage(CC.RED + "Usage: " + commandInfo.usage());
            return;
        }

        Player targetPlayer = plugin.getServer().getPlayer(commandArgs.getArgs()[0]);
        if (targetPlayer == null) {
            commandArgs.getSender().sendMessage(CC.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase(Locale.ENGLISH);
        PermissionConfigManager manager = plugin.getPermissionConfigManager();

        if ("list".equals(action)) {
            PermissionConfigManager.PermissionEntrySnapshot snapshot =
                    manager.getSnapshot(targetPlayer.getUniqueId());
            List<String> grants = snapshot.getGrants();
            List<String> denies = snapshot.getDenies();

            commandArgs.getSender().sendMessage(CC.AQUA + "Permission overrides for " + targetPlayer.getName() + ":");
            commandArgs.getSender().sendMessage(CC.GRAY + "  Grants: " +
                    (grants.isEmpty() ? CC.DARK_GRAY + "(none)" : CC.GREEN + String.join(", ", grants)));
            commandArgs.getSender().sendMessage(CC.GRAY + "  Denies: " +
                    (denies.isEmpty() ? CC.DARK_GRAY + "(none)" : CC.RED + String.join(", ", denies)));
            return;
        }

        if (commandArgs.length() < 3) {
            commandArgs.getSender().sendMessage(CC.RED + "Usage: " + commandInfo.usage());
            return;
        }

        String permissionNode = normalizePermission(commandArgs.getArgs()[2]);
        if (permissionNode.isEmpty()) {
            commandArgs.getSender().sendMessage(CC.RED + "Permission node cannot be empty.");
            return;
        }

        if ("add".equals(action)) {
            manager.setGranted(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(CC.GREEN + "Granted " + permissionNode + " to " + targetPlayer.getName() + ".");
            return;
        }

        if ("deny".equals(action)) {
            manager.setDenied(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(CC.GREEN + "Denied " + permissionNode + " for " + targetPlayer.getName() + ".");
            return;
        }

        if ("remove".equals(action)) {
            manager.removeOverride(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            commandArgs.getSender().sendMessage(CC.GREEN + "Removed override " + permissionNode + " from " + targetPlayer.getName() + ".");
            return;
        }

        commandArgs.getSender().sendMessage(CC.RED + "Invalid action. Use add, deny, remove, or list.");
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 0 || args.length() == 1) {
            return completeOnlinePlayers(args);
        }

        if (args.length() == 2) {
            return completeCurrentArg(args, ACTIONS);
        }

        if (args.length() == 3) {
            String action = args.getOptionalString(1).orElse("").toLowerCase(Locale.ENGLISH);
            if ("list".equals(action)) {
                return Collections.emptyList();
            }

            Set<String> registeredNodes = plugin.getCommandHandler() != null
                    ? plugin.getCommandHandler().getRegisteredPermissionNodes()
                    : Collections.emptySet();
            List<String> candidates = new ArrayList<>(registeredNodes);

            Player target = plugin.getServer().getPlayer(args.getOptionalString(0).orElse(""));
            if (target != null) {
                PermissionConfigManager.PermissionEntrySnapshot snapshot =
                        plugin.getPermissionConfigManager().getSnapshot(target.getUniqueId());
                candidates.addAll(snapshot.getGrants());
                candidates.addAll(snapshot.getDenies());
            }

            return completeCurrentArg(args, candidates);
        }

        return Collections.emptyList();
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





