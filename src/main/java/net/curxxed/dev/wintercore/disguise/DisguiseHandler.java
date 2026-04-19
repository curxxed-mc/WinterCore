package net.curxxed.dev.wintercore.disguise;

import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class DisguiseHandler {

    public final WinterCore plugin;
    public Map<String, SkinFetcher.SkinProperty> skinData;
    public Map<String, ItemStack> itemsData;
    private final Set<String> pendingFetches = ConcurrentHashMap.newKeySet();

    public DisguiseHandler(WinterCore plugin) {
        this.skinData  = new ConcurrentHashMap<>();
        this.itemsData = new HashMap<>();
        this.plugin    = plugin;
    }

    public void fetchSkinData(String name, Consumer<SkinFetcher.SkinProperty> callback) {
        String key = name.toLowerCase();
        SkinFetcher.SkinProperty cached = skinData.get(key);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        SkinFetcher.fetchSkin(name, (skin, err) -> {
            pendingFetches.remove(key);
            if (err != null) {
                Bukkit.getLogger().warning("Failed to fetch skin for " + name + ": " + err.getMessage());
                callback.accept(null);
                return;
            }
            skinData.put(key, skin);
            callback.accept(skin);
        });

        pendingFetches.add(key);
    }

    public void openDisguiseMenu(Player player, String targetName) {
        DisguiseMenu.setPendingTarget(player, targetName);
        String key = targetName.toLowerCase();
        if (!skinData.containsKey(key) && !pendingFetches.contains(key)) {
            fetchSkinData(targetName, skin -> {
                if (skin == null) {
                    Bukkit.getLogger().warning("[Disguise] Pre-fetch for '" + targetName + "' returned no skin.");
                }
            });
        }
        new DisguiseMenu(plugin, this, targetName).open(player);
    }

    public void sendPacket(Player player, Object packet) {
        Utilities.sendPacket(player, packet);
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
        return Utilities.getNMSClass(what);
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
            if (constructor.getParameterCount() == required) return constructor;
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
            Field minecraftServerField = connection.getClass().getDeclaredField("minecraftServer");
            minecraftServerField.setAccessible(true);
            Object minecraftServer = minecraftServerField.get(connection);
            Object playerlist = minecraftServer.getClass().getDeclaredMethod("getPlayerList").invoke(minecraftServer);
            Method moveToWorld = playerlist.getClass().getMethod("moveToWorld", EntityPlayer, int.class, boolean.class, Location.class, boolean.class);
            moveToWorld.invoke(playerlist, nmsPlayer, getDimensionOfWorld(minecraftServer, player.getWorld()), true, player.getLocation(), false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getDimensionOfWorld(Object minecraftServer, World of) throws Exception {
        Field worldsField = minecraftServer.getClass().getField("worlds");
        worldsField.setAccessible(true);
        List<Object> worlds = (List<Object>) worldsField.get(minecraftServer);
        for (Object worldServer : worlds) {
            Object world = safeCastTo(worldServer, getNMSClass("World"));
            Object worldData = world.getClass().getMethod("getWorldData").invoke(world);
            String name = (String) worldData.getClass().getMethod("getName").invoke(worldData);
            if (name.equals(of.getName()))
                return worldServer.getClass().getField("dimension").getInt(worldServer);
        }
        return 0;
    }

    public Object getPlayerHandle(Object invoke) {
        try {
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + Utilities.getServerVersion() + ".entity.CraftPlayer");
            return craftPlayer.getMethod("getHandle").invoke(invoke);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <T> T safeCastTo(Object obj, Class<T> to) {
        if (obj != null && to != null && to.isAssignableFrom(obj.getClass()))
            return to.cast(obj);
        return null;
    }

    public abstract void disguise(Player player, String rank, String name, String skin, Consumer<DisguiseCallback> callback);
    public abstract void unDisguise(Player player, boolean save, Consumer<DisguiseCallback> callback);
    public abstract void disguise(Player player, String targetName, Consumer<DisguiseCallback> callback);
    public abstract void undisguise(Player player, Consumer<DisguiseCallback> callback);
}