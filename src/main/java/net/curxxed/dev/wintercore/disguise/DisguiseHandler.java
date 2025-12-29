package net.curxxed.dev.wintercore.disguise;

import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DisguiseHandler {
    public final WinterCore plugin;
    public Map<String, SkinFetcher.SkinProperty> skinData;
    public Map<String, ItemStack> itemsData;

    public DisguiseHandler(WinterCore plugin) {
        this.skinData = new HashMap<>();
        this.itemsData = new HashMap<>();
        this.plugin = plugin;
    }

    public SkinFetcher.SkinProperty fetchSkinData(String name) {
        try {
            SkinFetcher.SkinProperty skin = SkinFetcher.fetchSkin(name);
            skinData.put(name.toLowerCase(), skin);
            return skin;
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to fetch skin for " + name + ": " + e.getMessage());
            return null;
        }
    }

    public void sendPacket(Player player, Object packet) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket",
                            Class.forName("net.minecraft.server." + Utilities.getServerVersion() + ".Packet"))
                    .invoke(playerConnection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeField(Object object, String fieldName, Object to) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Class<?> getNMSClass(String what) {
        try {
            return Class.forName("net.minecraft.server." + Utilities.getServerVersion() + "." + what);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean doesClassExists(String what) {
        try {
            Class.forName("net.minecraft.server." + Utilities.getServerVersion() + "." + what);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public Constructor<?> getConstructorWithParameterExact(Class<?> clazz, int required) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() == required)
                return constructor;
        }
        return clazz.getConstructors()[0];
    }

    public BufferedImage getSkinTexture(String name) throws Exception {
        return javax.imageio.ImageIO.read(new java.net.URL("https://minotar.net/avatar/" + name + "/8.png"));
    }

    public void moveToWorld(Player player) {
        try {
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = nmsPlayer.getClass().getDeclaredField("playerConnection").get(nmsPlayer);
            Class<?> EntityPlayer = Class.forName(nmsPlayer.getClass().getPackage().getName() + ".EntityPlayer");
            Field minecraftServer = connection.getClass().getDeclaredField("minecraftServer");
            minecraftServer.setAccessible(true);
            Object minecraftServerInitialized = minecraftServer.get(connection);
            Object playerlist = minecraftServerInitialized.getClass().getDeclaredMethod("getPlayerList").invoke(minecraftServerInitialized);
            Method moveToWorld = playerlist.getClass().getMethod("moveToWorld", EntityPlayer, int.class, boolean.class, Location.class, boolean.class);
            moveToWorld.invoke(playerlist, nmsPlayer, getDimensionOfWorld(minecraftServerInitialized, player.getWorld()), true, player.getLocation(), false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getDimensionOfWorld(Object minecraftServer, World of) throws Exception {
        Field worldsField = minecraftServer.getClass().getField("worlds");
        worldsField.setAccessible(true);
        java.util.List<Object> worlds = (java.util.List<Object>)worldsField.get(minecraftServer);
        for (Object worldServer : worlds) {
            Class<?> clazz = worldServer.getClass();
            Object world = safeCastTo(worldServer, getNMSClass("World"));
            Object worldData = world.getClass().getMethod("getWorldData").invoke(world);
            String name = (String)worldData.getClass().getMethod("getName").invoke(worldData);
            if (name.equals(of.getName()))
                return clazz.getField("dimension").getInt(worldServer);
        }
        return 0;
    }

    public Object getPlayerHandle(Object invoke) {
        try {
            Class<?> nmsEntityClass = Class.forName("org.bukkit.craftbukkit." + Utilities.getServerVersion() + ".entity.CraftPlayer");
            return nmsEntityClass.getMethod("getHandle").invoke(invoke);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T safeCastTo(Object obj, Class<T> to) {
        if (obj != null) {
            Class<?> c = obj.getClass();
            if (to.isAssignableFrom(c))
                return to.cast(obj);
        }
        return null;
    }
    public abstract DisguiseCallback disguise(Player player, String rank, String name, String skin) throws Exception;
    public abstract DisguiseCallback unDisguise(Player player, boolean paramBoolean) throws Exception;
    public abstract DisguiseCallback disguise(Player player, String targetName) throws Exception;
    public abstract DisguiseCallback undisguise(Player player) throws Exception;

    public void openRankSelectionGUI(Player player, String targetName) {
        Inventory gui = Bukkit.createInventory(null, 36, "Select disguise rank");
        List<String> ranks = plugin.getRankManager().getSortedRanks();
        ConfigurationSection ranksSection = plugin.getRankManager().getRanksSection();
        int slot = 0;
        for (String rank : ranks) {
            if (rank == null || rank.isEmpty()) continue;
            String colorCode = ranksSection.getString(rank + ".name-color", "&f");
            String translatedColorCode = CC.translate(colorCode);
            // Remove only italics from color code for preview, keep all other formats
            String cleanColorCode = colorCode.replaceAll("(?i)&o|§o", "");
            String translatedColorCodeNoItalic = CC.translate(cleanColorCode);
            // Use translatedColorCodeNoItalic for the prefix preview
            String prefix = ranksSection.getString(rank + ".prefix", "");
            DyeColor dyeColor = getDyeColorFromChatColor(ChatColor.getByChar(colorCode.replace("&", "").charAt(0)));
            ItemStack rankItem;
            if (ChatColor.getByChar(colorCode.replace("&", "").charAt(0)) == ChatColor.DARK_RED) {
                rankItem = new ItemStack(Material.STAINED_CLAY, 1, (short) 14);
            } else {
                rankItem = new ItemStack(Material.WOOL, 1, dyeColor.getWoolData());
            }
            ItemMeta meta = rankItem.getItemMeta();
            meta.setDisplayName(translatedColorCode + rank);
            List<String> lore = new ArrayList<>();
            lore.add(CC.translate("&7") + "§m------------------------");
            lore.add(CC.translate("&6") + "Prefix: " + CC.translate("&f") + translatedColorCodeNoItalic + CC.translate(prefix));
            lore.add(CC.translate("&7") + "§m------------------------");
            lore.add(CC.translate("&a") + "Click to Disguise as " + CC.translate("&b") + targetName + CC.translate("&a") + ".");
            meta.setLore(lore);
            rankItem.setItemMeta(meta);
            gui.setItem(slot++, rankItem);
        }
        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(CC.translate("&cCancel"));
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(35, cancelItem);
        player.openInventory(gui);
    }

    private DyeColor getDyeColorFromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK: return DyeColor.BLACK;
            case DARK_BLUE: return DyeColor.BLUE;
            case DARK_GREEN: return DyeColor.GREEN;
            case DARK_AQUA: return DyeColor.CYAN;
            case DARK_RED: return DyeColor.RED;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case GOLD: return DyeColor.ORANGE;
            case GRAY: return DyeColor.SILVER;
            case DARK_GRAY: return DyeColor.GRAY;
            case BLUE: return DyeColor.LIGHT_BLUE;
            case GREEN: return DyeColor.LIME;
            case AQUA: return DyeColor.LIGHT_BLUE;
            case RED: return DyeColor.RED;
            case LIGHT_PURPLE: return DyeColor.PINK;
            case YELLOW: return DyeColor.YELLOW;
            case WHITE: return DyeColor.WHITE;
            default: return DyeColor.WHITE;
        }
    }
}
