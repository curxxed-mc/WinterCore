package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.config.PermissionConfigManager;
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
            sendUsage(commandArgs.getSender());
            return;
        }

        Player targetPlayer = plugin.getServer().getPlayer(commandArgs.getArgs()[0]);
        if (targetPlayer == null) {
            send(commandArgs.getSender(), "general.player-not-found", "&cPlayer not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase(Locale.ENGLISH);
        PermissionConfigManager manager = plugin.getPermissionConfigManager();

        if ("list".equals(action)) {
            PermissionConfigManager.PermissionEntrySnapshot snapshot =
                    manager.getSnapshot(targetPlayer.getUniqueId());
            List<String> grants = snapshot.getGrants();
            List<String> denies = snapshot.getDenies();

            send(commandArgs.getSender(), "permission.list.header",
                    "&bPermission overrides for {target}:",
                    "{target}", targetPlayer.getName());
            send(commandArgs.getSender(), "permission.list.grants",
                    "&7  Grants: {grants}",
                    "{grants}", grants.isEmpty()
                            ? msg("permission.list.none", "&8(none)")
                            : msg("permission.list.grant-color", "&a") + String.join(", ", grants));
            send(commandArgs.getSender(), "permission.list.denies",
                    "&7  Denies: {denies}",
                    "{denies}", denies.isEmpty()
                            ? msg("permission.list.none", "&8(none)")
                            : msg("permission.list.deny-color", "&c") + String.join(", ", denies));
            return;
        }

        if (commandArgs.length() < 3) {
            sendUsage(commandArgs.getSender());
            return;
        }

        String permissionNode = normalizePermission(commandArgs.getArgs()[2]);
        if (permissionNode.isEmpty()) {
            send(commandArgs.getSender(), "permission.empty-node", "&cPermission node cannot be empty.");
            return;
        }

        if ("add".equals(action)) {
            manager.setGranted(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            send(commandArgs.getSender(), "permission.granted",
                    "&aGranted {permission} to {target}.",
                    "{permission}", permissionNode,
                    "{target}", targetPlayer.getName());
            return;
        }

        if ("deny".equals(action)) {
            manager.setDenied(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            send(commandArgs.getSender(), "permission.denied",
                    "&aDenied {permission} for {target}.",
                    "{permission}", permissionNode,
                    "{target}", targetPlayer.getName());
            return;
        }

        if ("remove".equals(action)) {
            manager.removeOverride(targetPlayer.getUniqueId(), permissionNode);
            refreshPlayerPermissions(targetPlayer);
            send(commandArgs.getSender(), "permission.removed",
                    "&aRemoved override {permission} from {target}.",
                    "{permission}", permissionNode,
                    "{target}", targetPlayer.getName());
            return;
        }

        send(commandArgs.getSender(), "permission.invalid-action", "&cInvalid action. Use add, deny, remove, or list.");
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
