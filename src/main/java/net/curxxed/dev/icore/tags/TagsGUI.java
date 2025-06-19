package net.curxxed.dev.icore.tags;

import net.curxxed.dev.icore.utils.CC;
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
import java.util.List;

public class TagsGUI implements Listener {
    private final TagsManager tagsManager;

    public TagsGUI(TagsManager tagsManager) {
        this.tagsManager = tagsManager;
    }

    public void openTagsGUI(Player player) {
        List<Tag> tags = tagsManager.getTagsSorted();
        Inventory gui = Bukkit.createInventory(null, 27, "Select a Tag");
        for (int i = 0; i < tags.size() && i < 27; i++) {
            Tag tag = tags.get(i);
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            String colorCode = TagsManager.colorNameToCode(tag.getColor());
            meta.setDisplayName(CC.translate(colorCode + tag.getName()));
            List<String> lore = new ArrayList<>();
            if (!tag.getPrefix().isEmpty()) {
                lore.add(CC.translate("&7Prefix: " + colorCode + tag.getPrefix()));
            }
            lore.add(CC.translate("&7Click to select this tag"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("Select a Tag")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            ItemMeta meta = clicked.getItemMeta();
            if (!meta.hasDisplayName()) return;
            String clickedName = CC.stripColor(meta.getDisplayName());
            Tag selectedTag = null;
            for (Tag tag : tagsManager.getTagsSorted()) {
                if (CC.stripColor(tag.getName()).equalsIgnoreCase(clickedName)) {
                    selectedTag = tag;
                    break;
                }
            }
            if (selectedTag == null) return;
            Player player = (Player) e.getWhoClicked();
            tagsManager.setPlayerTag(player.getUniqueId(), selectedTag.getId());
            player.closeInventory();
            player.sendMessage(CC.translate("&aSelected tag: ") + CC.translate(TagsManager.colorNameToCode(selectedTag.getColor()) + selectedTag.getName()));
        }
    }

    public void refresh() {
        String guiTitle = "Select a Tag";
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null &&
                    player.getOpenInventory().getTitle().equals(guiTitle)) {
                player.closeInventory();
                player.sendMessage(CC.translate("&eTags have been reloaded. Please reopen the tag menu."));
            }
        }
    }
}
