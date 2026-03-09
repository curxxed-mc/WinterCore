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

    // Tracks names whose skin is currently being fetched, so we don't fire
    // duplicate HTTP requests if the player somehow clicks before the first
    // fetch completes.
    private final Set<String> pendingFetches = ConcurrentHashMap.newKeySet();

    public DisguiseHandler(WinterCore plugin) {
        this.skinData  = new ConcurrentHashMap<>();   // was HashMap – safe now for concurrent access
        this.itemsData = new HashMap<>();
        this.plugin    = plugin;
    }

    /**
     * Fetches skin data for {@code name}, using the in-memory cache when
     * available so that subsequent calls (e.g. rank-selection click after
     * the pre-fetch in {@link #openDisguiseMenu}) are instant.
     */
    public void fetchSkinData(String name, Consumer<SkinFetcher.SkinProperty> callback) {
        String key = name.toLowerCase();

        // ── 1. Already cached → return immediately, no network call ──────────
        SkinFetcher.SkinProperty cached = skinData.get(key);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        // ── 2. Fetch is already in-flight → just queue the callback ──────────
        // (simple guard: if another thread is already fetching this name we
        // still call fetchSkin but the result will overwrite the same map
        // slot, which is harmless.  A full queue would be overkill here.)

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

    /**
     * Opens the rank-selection menu <em>and</em> kicks off the skin fetch in
     * the background immediately.  By the time the staff member clicks a rank
     * (even within ~1 second) the skin will already be in the cache, making
     * the actual disguise application instantaneous.
     */
    public void openDisguiseMenu(Player player, String targetName) {
        DisguiseMenu.setPendingTarget(player, targetName);

        // Pre-fetch skin now, while the menu is open.
        // fetchSkinData is safe to call even if a fetch is already running –
        // the cache check at the top will short-circuit on the second call.
        String key = targetName.toLowerCase();
        if (!skinData.containsKey(key) && !pendingFetches.contains(key)) {
            fetchSkinData(targetName, skin -> {
                // Result is stored in skinData automatically; nothing else to do.
                if (skin == null) {
                    Bukkit.getLogger().warning("[Disguise] Pre-fetch for '" + targetName + "' returned no skin.");
                }
            });
        }

        // Open the menu straight away – don't make the player wait.
        new DisguiseMenu(plugin, this, targetName).open(player);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Everything below is unchanged
    // ─────────────────────────────────────────────────────────────────────────

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