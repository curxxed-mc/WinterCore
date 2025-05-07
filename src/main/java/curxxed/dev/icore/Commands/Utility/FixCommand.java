package curxxed.dev.icore.Commands.Utility;

import curxxed.dev.icore.iCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class FixCommand implements CommandExecutor {

    private final iCore plugin;

    public FixCommand(iCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();

        // If the player doesn't have permission to use the command
        if (!player.hasPermission("iCore.fix")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }

        if (args.length == 0) {
            // Repair the item in hand
            ItemStack itemInHand = player.getInventory().getItemInHand();
            if (itemInHand != null && itemInHand.getType() != Material.AIR) {
                itemInHand.setDurability((short) 0); // Fully repair the item
                player.sendMessage(ChatColor.GREEN + "Your item has been repaired!");
            } else {
                player.sendMessage(ChatColor.RED + "You are not holding an item that can be repaired.");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            // Repair all items in inventory, including armor
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    item.setDurability((short) 0); // Fully repair the item
                }
            }
            // Repair armor
            for (ItemStack armorPiece : inventory.getArmorContents()) {
                if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                    armorPiece.setDurability((short) 0); // Fully repair the armor piece
                }
            }
            player.sendMessage(ChatColor.GREEN + "All items in your inventory, including armor, have been repaired!");
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /fix [all]");
        }

        return true;
    }
}
