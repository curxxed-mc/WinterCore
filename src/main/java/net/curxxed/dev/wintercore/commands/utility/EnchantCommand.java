package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.*;

@CommandInfo(
        name = "enchant",
        description = "Enchant the item in your hand.",
        usage = "/enchant <enchantment> [level]",
        inGameOnly = true,
        permission = {"wintercore.enchant", "WinterCore.enchant"}
)
public class EnchantCommand extends BaseCommand {

    private static final Map<String, Enchantment> ENCHANTMENT_MAP = new HashMap<>();
    private static final Set<String> TAB_NAMES = new TreeSet<>();

    static {
        for (Enchantment enchantment : Enchantment.values()) {
            registerEnchantment(enchantment);
        }

        alias("sharpness", "DAMAGE_ALL");
        alias("efficiency", "DIG_SPEED");
        alias("fortune", "LOOT_BONUS_BLOCKS");
        alias("unbreaking", "DURABILITY");
        alias("power", "ARROW_DAMAGE");
        alias("infinity", "ARROW_INFINITE");
        alias("flame", "ARROW_FIRE");
        alias("punch", "ARROW_KNOCKBACK");
        alias("protection", "PROTECTION_ENVIRONMENTAL");
        alias("feather_falling", "PROTECTION_FALL");
        alias("fire_aspect", "FIRE_ASPECT");
        alias("looting", "LOOT_BONUS_MOBS");
        alias("silk_touch", "SILK_TOUCH");
        alias("knockback", "KNOCKBACK");
        alias("respiration", "OXYGEN");
        alias("aqua_affinity", "WATER_WORKER");
        alias("bane_of_arthropods", "DAMAGE_ARTHROPODS");
        alias("smite", "DAMAGE_UNDEAD");
        alias("luck", "LUCK");
        alias("lure", "LURE");
        alias("depth_strider", "DEPTH_STRIDER");
        alias("thorns", "THORNS");
        alias("frost_walker", "FROST_WALKER");
        alias("mending", "MENDING");
        alias("binding_curse", "BINDING_CURSE");
        alias("vanishing_curse", "VANISHING_CURSE");
        alias("sweeping_edge", "SWEEPING_EDGE");

    }

    private static void registerEnchantment(Enchantment enchantment) {
        if (enchantment == null) return;

        String legacyName = enchantment.getName();
        if (legacyName != null && !legacyName.trim().isEmpty()) {
            register(legacyName, enchantment, false);
        }

        String key = getNamespacedKey(enchantment);
        if (key != null && !key.trim().isEmpty()) {
            String cleanKey = key.replace("minecraft:", "");
            register(key, enchantment, false);
            register(cleanKey, enchantment, true);
        } else if (legacyName != null) {
            register(legacyName, enchantment, true);
        }
    }

    private static void register(String name, Enchantment enchantment, boolean tabComplete) {
        String normalized = normalize(name);
        if (normalized.isEmpty()) return;

        ENCHANTMENT_MAP.put(normalized, enchantment);

        if (tabComplete) {
            TAB_NAMES.add(name.toLowerCase(Locale.ROOT));
        }
    }

    private static void alias(String modernAlias, String existingName) {
        Enchantment enchantment = ENCHANTMENT_MAP.get(normalize(existingName));
        if (enchantment != null) {
            register(modernAlias, enchantment, true);
            TAB_NAMES.remove(existingName.toLowerCase(Locale.ROOT));
        }
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replace("minecraft:", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    private static String getNamespacedKey(Enchantment enchantment) {
        try {
            Method method = Enchantment.class.getMethod("getKey");
            Object key = method.invoke(enchantment);
            return key == null ? null : key.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
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
            sendUsage(player);
            return;
        }

        ItemStack itemInHand = getItemInHandSafe(player);
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            send(player, "enchant.no-item", "&cYou are not holding any item.");
            return;
        }

        Enchantment enchantment = ENCHANTMENT_MAP.get(normalize(args[0]));
        if (enchantment == null) {
            send(player, "enchant.invalid-enchantment", "&cInvalid enchantment: {enchantment}",
                    "{enchantment}", args[0]);
            return;
        }

        int level = 1;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                send(player, "enchant.invalid-level", "&cInvalid level: {level}",
                        "{level}", args[1]);
                return;
            }
        }

        itemInHand.addUnsafeEnchantment(enchantment, level);
        setItemInHandSafe(player, itemInHand);

        send(player, "enchant.success", "&aYou have enchanted the item with {enchantment} (Level {level}).",
                "{enchantment}", args[0],
                "{level}", String.valueOf(level));

        if (level > enchantment.getMaxLevel()) {
            send(player, "enchant.unsafe-level-warning", "&eWARNING: This level exceeds the maximum vanilla cap.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            return completeCurrentArg(args, new ArrayList<>(TAB_NAMES));
        }

        if (args.length() == 2) {
            return completeCurrentArg(args, Arrays.asList("1", "2", "3", "4", "5", "10", "25", "50", "100"));
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("deprecation")
    private ItemStack getItemInHandSafe(Player player) {
        try {
            Method method = player.getInventory().getClass().getMethod("getItemInMainHand");
            return (ItemStack) method.invoke(player.getInventory());
        } catch (Exception ignored) {
            return player.getInventory().getItemInHand();
        }
    }

    @SuppressWarnings("deprecation")
    private void setItemInHandSafe(Player player, ItemStack item) {
        try {
            Method method = player.getInventory().getClass().getMethod("setItemInMainHand", ItemStack.class);
            method.invoke(player.getInventory(), item);
        } catch (Exception ignored) {
            player.getInventory().setItemInHand(item);
        }
    }
}
