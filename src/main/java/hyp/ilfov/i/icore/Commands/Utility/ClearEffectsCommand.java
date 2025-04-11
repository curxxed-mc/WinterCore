package hyp.ilfov.i.icore.Commands.Utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public class ClearEffectsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("@a")) {
            if (!sender.hasPermission("icore.cleareffects.all")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to clear everyone's effects.");
                return true;
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                for (PotionEffect effect : onlinePlayer.getActivePotionEffects()) {
                    onlinePlayer.removePotionEffect(effect.getType());
                }
                onlinePlayer.sendMessage(ChatColor.GREEN + "All your potion effects have been cleared.");
            }

            sender.sendMessage(ChatColor.GREEN + "You cleared all players' potion effects.");
            return true;
        }


        assert sender instanceof Player;
        Player player = (Player) sender;
        Player target = player;

        if (args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
        }

        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }

        target.sendMessage(ChatColor.GREEN + "Your potion effects have been cleared.");
        if (target != player) {
            player.sendMessage(ChatColor.GREEN + "You cleared " + target.getName() + "'s potion effects.");
        }

        return true;
    }
}
