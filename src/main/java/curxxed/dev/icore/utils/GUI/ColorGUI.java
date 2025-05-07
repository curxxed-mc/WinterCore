package curxxed.dev.icore.utils.GUI;



import curxxed.dev.icore.iCore;
import curxxed.dev.icore.utils.NMSUtils;
import lombok.Getter;
import org.bukkit.Bukkit;

import org.bukkit.ChatColor;

import org.bukkit.Material;

import org.bukkit.command.Command;

import org.bukkit.command.CommandExecutor;

import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;

import org.bukkit.event.inventory.InventoryClickEvent;

import org.bukkit.inventory.Inventory;

import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;

import java.util.List;



public class ColorGUI implements CommandExecutor, Listener {

    private final iCore plugin;
    @Getter
    public static final ColorGUI instance = getInstance();



    public ColorGUI(iCore plugin) {
        this.plugin = plugin;
    }


    @Override

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {

            sender.sendMessage("This command can only be used by players.");

            return true;

        }

        Player player = (Player) sender;

        openColorGUI(player);

        return true;

    }



    private void openColorGUI(Player player) {

        Inventory inv = Bukkit.createInventory(null, 9, "Select Chat Color");



        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.RED, "Red", (byte) 1));

        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.BLUE, "Blue", (byte) 4));

        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.GREEN, "Green", (byte) 2));

        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.YELLOW, "Yellow", (byte) 11));

        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.AQUA, "Aqua", (byte) 6));

        inv.addItem(createColorItem(player, Material.INK_SACK, ChatColor.LIGHT_PURPLE, "Pink", (byte) 9));



        player.openInventory(inv);

    }



    public ItemStack createColorItem(Player player, Material material, ChatColor color, String colorName, byte dyeData) {

        ItemStack item = new ItemStack(material, 1, dyeData);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(color + colorName);



        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + "Example:");

        lore.add(ChatColor.WHITE + player.getName() + ChatColor.RESET + ": " + color + "Hi! :)");

        meta.setLore(lore);



        item.setItemMeta(meta);

        return item;

    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Inventory inv = event.getInventory();

            // Use reflection to get the inventory title
            String title = NMSUtils.getInventoryTitle(event);

            if (title != null && title.equals("Select Chat Color")) {
                event.setCancelled(true);

                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.hasItemMeta()) {
                    ChatColor color = ChatColor.getByChar(clickedItem.getItemMeta().getDisplayName().charAt(1));
                    plugin.getRankManager().setMessageColorPreference(player, color);
                    player.sendMessage("Chat message color set to: " + color + color.name());
                    player.closeInventory();
                }
            }
        }
    }
}
