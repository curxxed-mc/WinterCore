package curxxed.dev.icore.Commands.Troll;

import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

public class TrollCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("icore.troll")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /troll <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline.");
            return true;
        }

        sendDemoMenu(target);
        player.sendMessage(ChatColor.GREEN + "Trolled " + target.getName() + " with the demo menu!");

        return true;
    }

    private void sendDemoMenu(Player target) {
        PacketPlayOutGameStateChange packet = new PacketPlayOutGameStateChange(5, 0); // 5 = Demo Message
        ((CraftPlayer) target).getHandle().playerConnection.sendPacket(packet);
    }
}

