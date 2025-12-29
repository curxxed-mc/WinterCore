package net.curxxed.dev.wintercore.menus;

import lombok.Getter;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandInfo(
        name = "chatcolor",
        permission = "WinterCore.commands.chatcolor",
        description = "Open the chat color selection menus.",
        usage = "/colorgui",
        inGameOnly = true
)
public class ChatColorSelectionMenu extends BaseCommand implements Listener {
    @Getter
    public static ChatColorSelectionMenu Instance;
    private final WinterCore plugin;
    private static final Map<String, String> COLOR_CODE_TO_NAME = new HashMap<>();
    private static final Map<String, Byte> COLOR_CODE_TO_DYE_DATA = new HashMap<>();
    static {
        COLOR_CODE_TO_DYE_DATA.put("&1", (byte) 4);  // Dark Blue
        COLOR_CODE_TO_DYE_DATA.put("&2", (byte) 2);  // Dark Green
        COLOR_CODE_TO_DYE_DATA.put("&3", (byte) 6);  // Dark Aqua
        COLOR_CODE_TO_DYE_DATA.put("&4", (byte) 1);  // Dark Red
        COLOR_CODE_TO_DYE_DATA.put("&5", (byte) 5);  // Purple
        COLOR_CODE_TO_DYE_DATA.put("&6", (byte) 14); // Gold
        COLOR_CODE_TO_DYE_DATA.put("&7", (byte) 8);  // Gray
        COLOR_CODE_TO_DYE_DATA.put("&9", (byte) 11); // Blue
        COLOR_CODE_TO_DYE_DATA.put("&a", (byte) 10); // Green
        COLOR_CODE_TO_DYE_DATA.put("&b", (byte) 6);  // Aqua
        COLOR_CODE_TO_DYE_DATA.put("&c", (byte) 1);  // Red
        COLOR_CODE_TO_DYE_DATA.put("&d", (byte) 9);  // Pink
        COLOR_CODE_TO_DYE_DATA.put("&e", (byte) 11); // Yellow
    }
    static {
        COLOR_CODE_TO_NAME.put("&1", "Dark Blue");
        COLOR_CODE_TO_NAME.put("&2", "Dark Green");
        COLOR_CODE_TO_NAME.put("&3", "Dark Aqua");
        COLOR_CODE_TO_NAME.put("&4", "Dark Red");
        COLOR_CODE_TO_NAME.put("&5", "Purple");
        COLOR_CODE_TO_NAME.put("&6", "Gold");
        COLOR_CODE_TO_NAME.put("&7", "Gray");
        COLOR_CODE_TO_NAME.put("&9", "Blue");
        COLOR_CODE_TO_NAME.put("&a", "Green");
        COLOR_CODE_TO_NAME.put("&b", "Aqua");
        COLOR_CODE_TO_NAME.put("&c", "Red");
        COLOR_CODE_TO_NAME.put("&d", "Pink");
        COLOR_CODE_TO_NAME.put("&e", "Yellow");

    }

    public ChatColorSelectionMenu(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        openColorGUI(player);
    }

    private void openColorGUI(Player player) {
        int size = ((COLOR_CODE_TO_NAME.size() - 1) / 9 + 1) * 9; // round up to nearest multiple of 9
        Inventory inv = Bukkit.createInventory(null, size, "Select Chat Color");
        for (Map.Entry<String, String> entry : COLOR_CODE_TO_NAME.entrySet()) {
            String colorCode = entry.getKey();
            String colorName = entry.getValue();
            byte dyeData = COLOR_CODE_TO_DYE_DATA.getOrDefault(colorCode, (byte) 0);
            inv.addItem(createColorItem(player, Material.INK_SACK, colorCode, colorName, dyeData));
        }
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
            String title = Utilities.getInventoryTitle(event);
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