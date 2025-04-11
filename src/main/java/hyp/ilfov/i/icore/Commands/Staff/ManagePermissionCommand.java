package hyp.ilfov.i.icore.Commands.Staff;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManagePermissionCommand implements CommandExecutor {

    private final Main plugin;
    private final PermissionManager permissionManager;

    public ManagePermissionCommand(Main plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin); // Initialize PermissionManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("iCore.managepermissions")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /permission <player> <add/remove> <permission>");
            return true;
        }

        Player targetPlayer = plugin.getServer().getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        String action = args[1].toLowerCase();
        String permission = args[2];

        if (action.equals("add")) {
            // Add the permission
            addPermission(targetPlayer, permission);
            sender.sendMessage(ChatColor.GREEN + "Successfully added permission " + permission + " to " + targetPlayer.getName());
        } else if (action.equals("remove")) {
            // Remove the permission
            removePermission(targetPlayer, permission);
            sender.sendMessage(ChatColor.GREEN + "Successfully removed permission " + permission + " from " + targetPlayer.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid action. Use 'add' or 'remove'.");
        }

        return true;
    }

    private void addPermission(Player player, String permission) {
        // Add the permission for the player (implement your logic here)
        // You can either update the player's permissions directly or add it to the player's rank
        // Example (directly adding the permission):
        player.addAttachment(plugin, permission, true);
    }

    private void removePermission(Player player, String permission) {
        // Remove the permission for the player (implement your logic here)
        // Example (directly removing the permission):
        player.addAttachment(plugin, permission, false);
    }
}
