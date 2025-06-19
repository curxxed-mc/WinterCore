package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.permissions.iCorePermissible;
import net.curxxed.dev.icore.permissions.iCorePermissibleInjector;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ManagePermissionCommand extends BaseCommand {

    private final iCore plugin;

    public ManagePermissionCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "permission",
            permission = "iCore.managepermissions",
            description = "Add or remove a permission from a player.",
            usage = "/permission <player> <add/remove> <permission>",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        if (commandArgs.length() != 3) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /permission <player> <add/remove> <permission>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (targetPlayer == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs(1).toLowerCase();
        String permission = commandArgs.getArgs(2);

        if (action.equals("add")) {
            addPermission(targetPlayer, permission);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Successfully added permission " + permission + " to " + targetPlayer.getName());
        } else if (action.equals("remove")) {
            removePermission(targetPlayer, permission);
            commandArgs.getSender().sendMessage(ChatColor.GREEN + "Successfully removed permission " + permission + " from " + targetPlayer.getName());
        } else {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Invalid action. Use 'add' or 'remove'.");
        }
    }

    private void addPermission(Player player, String permission) {
        try {
            iCorePermissible permissible = (iCorePermissible) iCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            permissible.addRawPermission(permission, true);
            permissible.recalculatePermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add permission: " + e.getMessage());
        }
    }

    private void removePermission(Player player, String permission) {
        try {
            iCorePermissible permissible = (iCorePermissible) iCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            permissible.clearPermissions();
            permissible.recalculatePermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove permission: " + e.getMessage());
        }
    }
}