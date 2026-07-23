package net.curxxed.dev.wintercore.utils;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Easily create itemstacks, without messing your hands.
 * @author NonameSL
 * @credit optimized by curxxed for WinterCore
 */
public class ItemBuilder {

    private final ItemStack is;

    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material to create the ItemBuilder with.
     */
    public ItemBuilder(Material m){
        this(m, 1);
    }

    /**
     * Create a new ItemBuilder over an existing itemstack.
     * @param is The itemstack to create the ItemBuilder over.
     */
    public ItemBuilder(ItemStack is){
        this.is = is;
    }

    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material of the item.
     * @param amount The amount of the item.
     */
    public ItemBuilder(Material m, int amount){
        this.is = new ItemStack(m, amount);
    }

    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material of the item.
     * @param amount The amount of the item.
     * @param durability The durability/data byte of the item.
     */
    public ItemBuilder(Material m, int amount, byte durability){
        this(m, amount, (short) durability);
    }

    /**
     * Create a new ItemBuilder from scratch.
     * @param m The material of the item.
     * @param amount The amount of the item.
     * @param durability The durability/data value of the item.
     */
    public ItemBuilder(Material m, int amount, short durability){
        this.is = new ItemStack(m, amount, durability);
    }

    /**
     * Clone the ItemBuilder into a completely new, independent instance.
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ItemBuilder clone(){
        return new ItemBuilder(this.is.clone());
    }

    /**
     * Change the durability of the item.
     */
    public ItemBuilder setDurability(short dur){
        this.is.setDurability(dur);
        return this;
    }

    /**
     * Set the displayname of the item (Automatically colored via CC).
     */
    public ItemBuilder setName(String name){
        ItemMeta im = this.is.getItemMeta();
        if (im != null) {
            im.setDisplayName(CC.translate(name));
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Add an unsafe enchantment.
     */
    public ItemBuilder addUnsafeEnchantment(Enchantment ench, int level){
        this.is.addUnsafeEnchantment(ench, level);
        return this;
    }

    /**
     * Remove a certain enchant from the item.
     */
    public ItemBuilder removeEnchantment(Enchantment ench){
        this.is.removeEnchantment(ench);
        return this;
    }

    /**
     * Set the skull owner for the item. Safely ignores non-skulls.
     */
    public ItemBuilder setSkullOwner(String owner){
        ItemMeta im = this.is.getItemMeta();
        if (im instanceof SkullMeta) {
            ((SkullMeta) im).setOwner(owner);
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Add a safe enchant to the item.
     */
    public ItemBuilder addEnchant(Enchantment ench, int level){
        ItemMeta im = this.is.getItemMeta();
        if (im != null) {
            im.addEnchant(ench, level, true);
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Add multiple enchants at once.
     */
    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments){
        this.is.addEnchantments(enchantments);
        return this;
    }

    /**
     * Sets infinity durability on the item by making it unbreakable via Spigot.
     */
    public ItemBuilder setInfinityDurability(){
        ItemMeta meta = this.is.getItemMeta();
        if (meta != null) {
            meta.spigot().setUnbreakable(true);
            this.is.setItemMeta(meta);
        }
        return this;
    }

    /**
     * Re-sets the lore (Automatically colored via CC).
     */
    public ItemBuilder setLore(String... lore){
        return setLore(Arrays.asList(lore));
    }

    /**
     * Re-sets the lore (Automatically colored via CC).
     */
    public ItemBuilder setLore(List<String> lore) {
        ItemMeta im = this.is.getItemMeta();
        if (im != null) {
            im.setLore(CC.translate(lore));
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Remove a specific lore line.
     */
    public ItemBuilder removeLoreLine(String line){
        ItemMeta im = this.is.getItemMeta();
        if (im != null && im.hasLore()) {
            List<String> lore = new ArrayList<>(im.getLore());
            String coloredLine = CC.translate(line);
            if (lore.remove(coloredLine) || lore.remove(line)) {
                im.setLore(lore);
                this.is.setItemMeta(im);
            }
        }
        return this;
    }

    /**
     * Remove a lore line at a specific index.
     */
    public ItemBuilder removeLoreLine(int index){
        ItemMeta im = this.is.getItemMeta();
        if (im != null && im.hasLore()) {
            List<String> lore = new ArrayList<>(im.getLore());
            if (index >= 0 && index < lore.size()) {
                lore.remove(index);
                im.setLore(lore);
                this.is.setItemMeta(im);
            }
        }
        return this;
    }

    /**
     * Add a single lore line to the bottom of the item.
     */
    public ItemBuilder addLoreLine(String line){
        ItemMeta im = this.is.getItemMeta();
        if (im != null) {
            List<String> lore = im.hasLore() ? new ArrayList<>(im.getLore()) : new ArrayList<>();
            lore.add(CC.translate(line));
            im.setLore(lore);
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Inserts or appends a lore line safely at a specific index.
     */
    public ItemBuilder addLoreLine(String line, int pos){
        ItemMeta im = this.is.getItemMeta();
        if (im != null) {
            List<String> lore = im.hasLore() ? new ArrayList<>(im.getLore()) : new ArrayList<>();
            String coloredLine = CC.translate(line);
            if (pos >= 0 && pos <= lore.size()) {
                lore.add(pos, coloredLine);
            } else {
                lore.add(coloredLine);
            }
            im.setLore(lore);
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Sets the dye color on an item using sub-id data.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder setDyeColor(DyeColor color){
        this.is.setDurability(color.getData());
        return this;
    }

    /**
     * Sets the dye color of a wool item. Works only on wool.
     * @deprecated Use {@link #setDyeColor(DyeColor)} instead.
     */
    @Deprecated
    public ItemBuilder setWoolColor(DyeColor color){
        if (this.is.getType() == Material.WOOL) {
            this.is.setDurability(color.getData());
        }
        return this;
    }

    /**
     * Sets the armor color of a leather armor piece. Safely ignores non-leather items.
     */
    public ItemBuilder setLeatherArmorColor(Color color){
        ItemMeta im = this.is.getItemMeta();
        if (im instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) im).setColor(color);
            this.is.setItemMeta(im);
        }
        return this;
    }

    /**
     * Retrieves the completed itemstack from the ItemBuilder.
     */
    public ItemStack toItemStack(){
        return this.is;
    }
}
