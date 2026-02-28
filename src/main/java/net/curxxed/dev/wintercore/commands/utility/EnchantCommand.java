package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@CommandInfo(
        name = "enchant",
        permission = "WinterCore.enchant",
        description = "Enchant the item in your hand.",
        usage = "/enchant <enchantment> [level]",
        inGameOnly = true
)
public class EnchantCommand extends BaseCommand {

    private static final Map<String, Enchantment> ENCHANTMENT_MAP = new HashMap<>();

    static {
        addEnchant("sharpness",           Enchantment.DAMAGE_ALL);
        addEnchant("efficiency",          Enchantment.DIG_SPEED);
        addEnchant("fortune",             Enchantment.LOOT_BONUS_BLOCKS);
        addEnchant("unbreaking",          Enchantment.DURABILITY);
        addEnchant("power",               Enchantment.ARROW_DAMAGE);
        addEnchant("infinity",            Enchantment.ARROW_INFINITE);
        addEnchant("flame",               Enchantment.ARROW_FIRE);
        addEnchant("punch",               Enchantment.ARROW_KNOCKBACK);
        addEnchant("protection",          Enchantment.PROTECTION_ENVIRONMENTAL);
        addEnchant("feather_falling",     Enchantment.PROTECTION_FALL);
        addEnchant("fire_aspect",         Enchantment.FIRE_ASPECT);
        addEnchant("looting",             Enchantment.LOOT_BONUS_MOBS);
        addEnchant("silk_touch",          Enchantment.SILK_TOUCH);
        addEnchant("knockback",           Enchantment.KNOCKBACK);
        addEnchant("respiration",         Enchantment.OXYGEN);
        addEnchant("aqua_affinity",       Enchantment.WATER_WORKER);
        addEnchant("bane_of_arthropods",  Enchantment.DAMAGE_ARTHROPODS);
        addEnchant("smite",               Enchantment.DAMAGE_UNDEAD);
        addEnchant("luck",                Enchantment.LUCK);
        addEnchant("lure",                Enchantment.LURE);
        addEnchantByName("depth_strider",    "DEPTH_STRIDER");
        addEnchantByName("thorns",           "THORNS");
        addEnchantByName("frost_walker",     "FROST_WALKER");
        addEnchantByName("mending",          "MENDING");
        addEnchantByName("binding_curse",    "BINDING_CURSE");
        addEnchantByName("vanishing_curse",  "VANISHING_CURSE");
    }

    private static void addEnchant(String name, Enchantment enchantment) {
        if (enchantment != null) {
            ENCHANTMENT_MAP.put(name, enchantment);
        }
    }

    private static void addEnchantByName(String alias, String mojangName) {
        Enchantment enchantment = Enchantment.getByName(mojangName);
        if (enchantment != null) {
            ENCHANTMENT_MAP.put(alias, enchantment);
        }
    }

    public EnchantCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /enchant <enchantment> [level]");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You are not holding any item.");
            return;
        }

        String enchantmentName = args[0].toLowerCase();
        Enchantment enchantment = ENCHANTMENT_MAP.get(enchantmentName);

        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Invalid enchantment: " + args[0]);
            return;
        }

        int level = 1;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level: " + args[1]);
                return;
            }
        }

        itemInHand.addUnsafeEnchantment(enchantment, level);
        player.sendMessage(ChatColor.GREEN + "You have enchanted the item with " + enchantmentName + " (Level " + level + ").");
        if (level > enchantment.getMaxLevel()) {
            player.sendMessage(ChatColor.YELLOW + "WARNING: This level exceeds the maximum vanilla cap.");
        }
    }
}