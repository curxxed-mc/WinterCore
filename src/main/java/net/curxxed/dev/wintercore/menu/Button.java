package net.curxxed.dev.wintercore.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class Button {

    private final ItemStack item;
    private final Consumer<InventoryClickEvent> action;

    public Button(ItemStack item, Consumer<InventoryClickEvent> action) {
        this.item = item;
        this.action = action;
    }

    public Button(ItemStack item) {
        this(item, null);
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean hasAction() {
        return action != null;
    }

    public void onClick(InventoryClickEvent event) {
        if (action != null) action.accept(event);
    }
}