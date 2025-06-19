package net.curxxed.dev.icore.gui;

import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.NMSUtils;
import net.curxxed.dev.icore.utils.CC;
import lombok.Getter;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ColorGUI extends BaseCommand implements Listener {
    @Getter
    public static ColorGUI Instance;
    private final iCore plugin;
    private static final Map<String, String> COLOR_CODE_TO_NAME = new HashMap<>();
    static {
        COLOR_CODE_TO_NAME.put("&c", "Red");
        COLOR_CODE_TO_NAME.put("&9", "Blue");
        COLOR_CODE_TO_NAME.put("&a", "Green");
        COLOR_CODE_TO_NAME.put("&e", "Yellow");
        COLOR_CODE_TO_NAME.put("&b", "Aqua");
        COLOR_CODE_TO_NAME.put("&d", "Pink");
        COLOR_CODE_TO_NAME.put("&6", "Gold");
    }

    public ColorGUI(iCore plugin) {
        this.plugin = plugin;
    }

    @Command(
            name = "colorgui",
            permission = "iCore.colorgui",
            description = "Open the chat color selection gui.",
            usage = "/colorgui",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        openColorGUI(player);
    }

    private void openColorGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "Select Chat Color");
        inv.addItem(createColorItem(player, Material.INK_SACK, "&c", "Red", (byte) 1));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&9", "Blue", (byte) 4));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&a", "Green", (byte) 2));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&e", "Yellow", (byte) 11));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&b", "Aqua", (byte) 6));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&d", "Pink", (byte) 9));
        inv.addItem(createColorItem(player, Material.INK_SACK, "&6", "Gold", (byte) 14));
        player.openInventory(inv);
    }

    public ItemStack createColorItem(Player player, Material material, String colorCode, String colorName, byte dyeData) {
        ItemStack item = new ItemStack(material, 1, dyeData);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.translate(colorCode + colorName));
        List<String> lore = new ArrayList<>();
        lore.add(CC.translate("&7Example:"));
        lore.add(CC.translate("&f" + player.getName() + "&r: " + colorCode + "Hi! :)"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            String title = NMSUtils.getInventoryTitle(event);
            if (title != null && title.equals("Select Chat Color")) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta()) {
                    String displayName = clickedItem.getItemMeta().getDisplayName();
                    if (displayName != null && displayName.length() >= 2 && displayName.charAt(0) == '§') {
                        String colorCode = "&" + displayName.charAt(1);
                        plugin.getRankManager().setMessageColorPreference(player, colorCode);
                        String colorName = COLOR_CODE_TO_NAME.getOrDefault(colorCode, colorCode);
                        player.sendMessage(CC.translate("&aChat message color set to: " + colorName));
                        player.closeInventory();
                    }
                }
            }
        }
    }
}