package curxxed.dev.icore.Commands.Staff;

import curxxed.dev.icore.listeners.FreezeListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FreezeCommand implements CommandExecutor {
    private final FreezeListener freezeListener;
    private final String discordLink = "discord.gg/example";

    public FreezeCommand(FreezeListener freezeListener) {
        this.freezeListener = freezeListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("iCore.freeze")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /freeze <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        if (freezeListener.isFrozen(target)) {
            freezeListener.unfreezePlayer(target, player); // Pass both target and staff (player)
            player.sendMessage(ChatColor.GREEN + "Unfroze " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "You have been unfrozen.");
        } else {
            freezeListener.freezePlayer(target, player); // Pass both target and staff (player)
            player.sendMessage(ChatColor.RED + "Froze " + target.getName() + ".");
            target.sendMessage(ChatColor.RED + "You have been frozen! Join our Discord for more information." + ChatColor.AQUA + discordLink);
        }

        return true;
    }
}
