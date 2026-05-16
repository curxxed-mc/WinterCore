package net.curxxed.dev.wintercore.menu;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ButtonBuilder {

    private Material material = Material.STONE;
    private short data = 0;
    private int amount = 1;
    private String name = null;
    private final List<String> lore = new ArrayList<>();
    private Consumer<InventoryClickEvent> action = null;

    private ButtonBuilder() {}

    public static ButtonBuilder of(Material material) {
        ButtonBuilder b = new ButtonBuilder();
        b.material = material;
        return b;
    }

    public ButtonBuilder data(short data) {
        this.data = data;
        return this;
    }


    public ButtonBuilder data(byte data) {
        this.data = data;
        return this;
    }

    public ButtonBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }


    public ButtonBuilder name(String name) {
        this.name = CC.translate(name);
        return this;
    }

    public ButtonBuilder lore(String... lines) {
        Arrays.stream(lines).map(CC::translate).forEach(this.lore::add);
        return this;
    }

    public ButtonBuilder lore(List<String> lines) {
        lines.stream().map(CC::translate).forEach(this.lore::add);
        return this;
    }

    public ButtonBuilder action(Consumer<InventoryClickEvent> action) {
        this.action = action;
        return this;
    }

    public Button build() {
        ItemBuilder itemBuilder = new ItemBuilder(material, amount, (byte) data);
        if (name != null) {
            itemBuilder.setName(name);
        }
        if (!lore.isEmpty()) {
            itemBuilder.setLore(lore);
        }
        ItemStack item = itemBuilder.toItemStack();
        return new Button(item, action);
    }
}