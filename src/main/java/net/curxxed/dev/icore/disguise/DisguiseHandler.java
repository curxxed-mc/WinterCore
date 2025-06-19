package net.curxxed.dev.icore.disguise;

import net.curxxed.dev.icore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.NMSUtils;
import net.curxxed.dev.icore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DisguiseHandler {
    public final iCore plugin;
    public Map<String, SkinFetcher.SkinProperty> skinData; // In-memory, not persistent
    public Map<String, ItemStack> itemsData;

    public DisguiseHandler(iCore plugin) {
        this.skinData = new HashMap<>();
        this.itemsData = new HashMap<>();
        this.plugin = plugin;
    }

    // Fetches skin data for a player name using SkinFetcher
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
                            Class.forName("net.minecraft.server." + NMSUtils.getServerVersion() + ".Packet"))
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
            return Class.forName("net.minecraft.server." + NMSUtils.getServerVersion() + "." + what);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean doesClassExists(String what) {
        try {
            Class.forName("net.minecraft.server." + NMSUtils.getServerVersion() + "." + what);
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
            Class<?> nmsEntityClass = Class.forName("org.bukkit.craftbukkit." + NMSUtils.getServerVersion() + ".entity.CraftPlayer");
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

    public void openRankSelectionGUI(Player player, String name) {
        Inventory gui = Bukkit.createInventory(null, 9, "Select a rank");
        List<String> ranks = plugin.getRankManager().getAvailableRanks();

        for (int i = 0; i < ranks.size() && i < 9; i++) {
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ranks.get(i));
            item.setItemMeta(meta);
            gui.setItem(i, item);
        }
        player.openInventory(gui);
    }
}

