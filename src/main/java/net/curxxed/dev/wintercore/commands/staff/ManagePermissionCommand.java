package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissible;
import net.curxxed.dev.wintercore.permissions.WinterCorePermissibleInjector;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "permission",
            permission = "WinterCore.managepermissions",
            description = "Add or remove a permission from a player.",
            usage = "/permission <player> <add/remove> <permission>",
            inGameOnly = false
    
    )
public class ManagePermissionCommand extends BaseCommand {

    private final WinterCore plugin;

    public ManagePermissionCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() != 3) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /permission <player> <add/remove> <permission>");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (targetPlayer == null) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        String action = commandArgs.getArgs()[1].toLowerCase();
        String permission = commandArgs.getArgs()[2];

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
            WinterCorePermissible permissible = (WinterCorePermissible) WinterCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            permissible.addRawPermission(permission, true);
            permissible.recalculatePermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add permission: " + e.getMessage());
        }
    }

    private void removePermission(Player player, String permission) {
        try {
            WinterCorePermissible permissible = (WinterCorePermissible) WinterCorePermissibleInjector.HUMAN_ENTITY_PERMISSIBLE_FIELD.get(player);
            permissible.clearPermissions();
            permissible.recalculatePermissions();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove permission: " + e.getMessage());
        }
    }
}