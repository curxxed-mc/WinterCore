package net.curxxed.dev.wintercore.menu;

import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PaginatedMenu extends Menu {

    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int INFO_SLOT = 49;

    protected int page = 0;

    @Override
    public int getSize() {
        return 54;
    }

    public abstract List<Button> getAllButtons(Player player);

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        List<Button> all = getAllButtons(player);
        int perPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Map<Integer, Button> buttons = new HashMap<>();

        int start = page * perPage;
        int end = Math.min(start + perPage, all.size());
        for (int i = start; i < end; i++) {
            buttons.put(i - start, all.get(i));
        }

        if (page > 0) {
            buttons.put(PREV_SLOT, new Button(buildNavItem(Material.ARROW, CC.translate("&aPrevious Page")), e -> {
                page--;
                refresh((Player) e.getWhoClicked());
            }));
        }

        if (page < totalPages - 1) {
            buttons.put(NEXT_SLOT, new Button(buildNavItem(Material.ARROW, CC.translate("&aNext Page")), e -> {
                page++;
                refresh((Player) e.getWhoClicked());
            }));
        }

        ItemStack info = buildNavItem(Material.PAPER, CC.translate("&ePage " + (page + 1) + " of " + totalPages));
        buttons.put(INFO_SLOT, new Button(info));

        return buttons;
    }

    private ItemStack buildNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}