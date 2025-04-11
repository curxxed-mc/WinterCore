package hyp.ilfov.i.icore.Commands.misc;

import hyp.ilfov.i.icore.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MoreCommand implements CommandExecutor {

    private final Main plugin;

    public MoreCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // If the player doesn't have permission to use the command
        if (!player.hasPermission("iCore.more")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }

        // Get the item the player is holding
        ItemStack itemInHand = player.getItemInHand(); // Works in 1.8.8
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding any item.");
            return false;
        }

        // Create a new ItemStack of the same type with a full stack (64 items)
        ItemStack newItemStack = new ItemStack(itemInHand.getType(), 64);

        // Add the new stack to the player's inventory
        player.getInventory().addItem(newItemStack);

        // Notify the player
        player.sendMessage(ChatColor.GREEN + "You have received a stack of " + itemInHand.getType().name() + "!");
        return true;
    }
}
