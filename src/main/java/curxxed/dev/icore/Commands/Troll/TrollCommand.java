package curxxed.dev.icore.Commands.Troll;

import curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

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
        try {
            // Dynamically load PacketPlayOutGameStateChange
            Class<?> packetClass = NMSUtils.getNMSClass("PacketPlayOutGameStateChange");

            // Create the packet instance (5 = Demo Message, 0 = State)
            Object packet = NMSUtils.createInstance(packetClass, 5, 0);

            // Send the packet to the target player
            sendPacket(target, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            // Dynamically access CraftPlayer and playerConnection
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + NMSUtils.getServerVersion() + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);

            Class<?> playerConnectionClass = handle.getClass().getField("playerConnection").getType();
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

            // Dynamically invoke sendPacket
            Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", NMSUtils.getNMSClass("Packet"));
            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}