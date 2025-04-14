package curxxed.dev.icore.Commands.Utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class ThruCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("iCore.thru")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Get the block the player is facing
        BlockIterator iterator = new BlockIterator(player, 5); // Scan up to 5 blocks ahead
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();

            if (block.getType().isSolid()) { // If it's a solid block
                Vector direction = player.getLocation().getDirection().normalize();
                Vector newPosition = block.getLocation().toVector().add(direction).add(direction); // Move 2 blocks ahead

                // Ensure the new location is safe
                if (isSafeLocation(newPosition)) {
                    player.teleport(newPosition.toLocation(player.getWorld()));
                    player.sendMessage(ChatColor.AQUA + "There you go.");
                } else {
                    player.sendMessage(ChatColor.RED + "The other side is blocked!");
                }
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "No block found in front of you.");
        return true;
    }

    // Check if the new location is safe (not inside a solid block)
    private boolean isSafeLocation(Vector position) {
        org.bukkit.block.Block block = position.toLocation(Bukkit.getWorlds().get(0)).getBlock();
        org.bukkit.block.Block aboveBlock = position.toLocation(Bukkit.getWorlds().get(0)).add(0, 1, 0).getBlock();
        return !block.getType().isSolid() && !aboveBlock.getType().isSolid();
    }
}
