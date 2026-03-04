package net.curxxed.dev.wintercore.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public abstract class Menu {

    public abstract String getTitle();

    public abstract int getSize();

    public abstract Map<Integer, Button> getButtons(Player player);

    public void onOpen(Player player) {}

    public void onClose(Player player) {}

    public void open(Player player) {
        MenuManager.getInstance().openMenu(player, this);
    }

    public Inventory buildInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, getSize(), getTitle());
        Map<Integer, Button> buttons = getButtons(player);
        if (buttons != null) {
            for (Map.Entry<Integer, Button> entry : buttons.entrySet()) {
                if (entry.getKey() >= 0 && entry.getKey() < getSize() && entry.getValue() != null) {
                    inventory.setItem(entry.getKey(), entry.getValue().getItem());
                }
            }
        }
        return inventory;
    }

    public void refresh(Player player) {
        MenuManager.getInstance().refreshMenu(player, this);
    }
}