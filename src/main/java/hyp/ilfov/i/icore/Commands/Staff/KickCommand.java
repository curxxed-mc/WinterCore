package hyp.ilfov.i.icore.Commands.Staff;

import hyp.ilfov.i.icore.Main;
import hyp.ilfov.i.icore.utils.PunishmentManager;
import org.bukkit.BanList;
import org.bukkit.BanList.Type;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {
    private final Main plugin;
    private final PunishmentManager punishmentManager;

    public KickCommand(Main plugin) {
        this.plugin = plugin;
        this.punishmentManager = new PunishmentManager(plugin);  // Initialize PunishmentManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow players with the correct permission
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // Check if player has permission to kick others
        if (!player.hasPermission("iCore.kick")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to kick players.");
            return false;
        }

        // Ensure player specifies a target player and reason
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Please specify a player to kick.");
            return false;
        }

        Player target = plugin.getServer().getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return false;
        }

        // If the player provided a reason
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";


        // Add the kick to the punishment data (we assume "kick" type is used here)
        punishmentManager.addPunishment(target.getName(), "kicks", reason, player.getName(), null);

        // Send a message to the player who was kicked
        target.kickPlayer(ChatColor.RED + "You have been kicked for: " + reason);

        // Notify the sender (person issuing the kick)
        player.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been kicked for: " + reason);

        return true;
    }
}
