package hyp.ilfov.i.icore.Commands.Utility;

import hyp.ilfov.i.icore.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

public class EnchantCommand implements CommandExecutor {

    private final Main plugin;

    public EnchantCommand(Main plugin) {
        this.plugin = plugin;
    }

    private static final Map<String, Enchantment> ENCHANTMENT_MAP = new HashMap<>();

    static {
        // Add common enchantments (store keys in lowercase for case-insensitivity)
        ENCHANTMENT_MAP.put("sharpness", Enchantment.DAMAGE_ALL);
        ENCHANTMENT_MAP.put("efficiency", Enchantment.DIG_SPEED);
        ENCHANTMENT_MAP.put("fortune", Enchantment.LOOT_BONUS_BLOCKS);
        ENCHANTMENT_MAP.put("unbreaking", Enchantment.DURABILITY);
        ENCHANTMENT_MAP.put("power", Enchantment.ARROW_DAMAGE);
        ENCHANTMENT_MAP.put("infinity", Enchantment.ARROW_INFINITE);
        ENCHANTMENT_MAP.put("flame", Enchantment.ARROW_FIRE);
        ENCHANTMENT_MAP.put("punch", Enchantment.ARROW_KNOCKBACK);
        ENCHANTMENT_MAP.put("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
        ENCHANTMENT_MAP.put("feather_falling", Enchantment.PROTECTION_FALL);
        ENCHANTMENT_MAP.put("fire_aspect", Enchantment.FIRE_ASPECT);
        // Add more as needed
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // Check if the player has permission to use the command
        if (!player.hasPermission("iCore.enchant")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }

        // Ensure the player is holding an item
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding any item.");
            return false;
        }

        // Ensure the enchantment name and level are provided
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /enchant <player> <enchantment> [level]");
            return false;
        }

        // Get the enchantment name and map to the Bukkit Enchantment
        String enchantmentName = args[1].toLowerCase(); // Convert to lowercase for case-insensitivity
        Enchantment enchantment = ENCHANTMENT_MAP.get(enchantmentName);

        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Invalid enchantment: " + args[1]);
            return false;
        }

        // Default level is 1 if no level is provided
        int level = 1;
        if (args.length >= 3) {
            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level: " + args[2]);
                return false;
            }
        }

        // Apply the enchantment with the specified level
        itemInHand.addUnsafeEnchantment(enchantment, level);

        // Send confirmation to the player
        player.sendMessage(ChatColor.GREEN + "You have enchanted the item with " + args[1] + " (Level " + level + ").");
        int maxLevel = enchantment.getMaxLevel();
        if (level > maxLevel) {
            player.sendMessage(ChatColor.YELLOW + "WARNING: This level exceeds the maximum vanilla cap.");
        }

        return true;
    }
}
