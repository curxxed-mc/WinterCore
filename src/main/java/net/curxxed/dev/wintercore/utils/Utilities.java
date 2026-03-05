package net.curxxed.dev.wintercore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

    private static final String SERVER_VERSION;
    public static final boolean IS_LEGACY;
    public static final boolean IS_1_7;
    private static Field pingField = null;

    static {
        SERVER_VERSION = detectServerVersion();
        IS_LEGACY = detectMinecraftMajor() < 17;
        IS_1_7 = detectMinecraftMajor() == 7;
    }

    private static String detectServerVersion() {
        try {
            //noinspection JavaReflectionMemberAccess -- for newer versions
            Method m = Bukkit.class.getMethod("getMinecraftVersion");
            return (String) m.invoke(null);
        } catch (Exception ignored) {}

        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = pkg.split("\\.");
            if (parts.length > 3) {
                return parts[3];
            }
        } catch (Exception ignored) {}

        return "unknown";
    }

    private static int detectMinecraftMajor() {
        try {
            if (SERVER_VERSION.contains(".")) {
                String[] parts = SERVER_VERSION.split("\\.");
                return Integer.parseInt(parts[1]);
            }
            if (SERVER_VERSION.startsWith("v")) {
                String[] parts = SERVER_VERSION.split("_");
                return Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}

        return 8;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    public static double[] getTPS() {
        try {
            Method paperGetTPS = Bukkit.getServer().getClass().getMethod("getTPS");
            return (double[]) paperGetTPS.invoke(Bukkit.getServer());
        } catch (Exception ignored) {}
        try {
            Object minecraftServer;

            if (IS_LEGACY) {
                String pkg = Bukkit.getServer().getClass().getPackage().getName();
                String version = pkg.substring("org.bukkit.craftbukkit.".length());

                minecraftServer = Class //CraftBukkit Mappings
                        .forName("net.minecraft.server." + version + ".MinecraftServer")
                        .getMethod("getServer")
                        .invoke(null);
            } else {
                minecraftServer = Class //Mojang Mappings
                        .forName("net.minecraft.server.MinecraftServer")
                        .getMethod("getServer")
                        .invoke(null);
            }

            Field tpsField = minecraftServer.getClass().getField("recentTps");
            return (double[]) tpsField.get(minecraftServer);

        } catch (Exception e) {
            return new double[]{-1D, -1D, -1D};
        }
    }


    public static Class<?> getCraftBukkitClass(String path) throws ClassNotFoundException {
        if (IS_LEGACY) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            String version = pkg.substring("org.bukkit.craftbukkit.".length());
            return Class.forName("org.bukkit.craftbukkit." + version + "." + path);
        } else {
            return Class.forName("org.bukkit.craftbukkit." + path);
        }
    }

    public static Object getEntityPlayer(Player player) throws Exception {
        Class<?> craftPlayer = getCraftBukkitClass("entity.CraftPlayer");
        Method getHandle = craftPlayer.getMethod("getHandle");
        return getHandle.invoke(craftPlayer.cast(player));
    }


    public static int getPing(Player player) {
        try {
            if (IS_LEGACY) {
                Object entityPlayer = getEntityPlayer(player);
                if (pingField == null) {
                    pingField = entityPlayer.getClass().getField("ping");
                }
                return pingField.getInt(entityPlayer);
            } else {
                //noinspection JavaReflectionMemberAccess
                Method getPing = Player.class.getMethod("getPing");
                return (int) getPing.invoke(player);
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public static @NotNull List<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }


    public static void logBootBanner() {
        Bukkit.getLogger().info(
                "Utilities initialized → Version: " + SERVER_VERSION +
                        " | Mode: " + (IS_LEGACY ? "Legacy (≤1.16)" : "Modern (1.17+)")
        );
    }

    public static String getInventoryTitle(InventoryClickEvent event) {
        try {
            if (IS_LEGACY) {
                Method getTitle = event.getInventory().getClass().getMethod("getTitle");
                return (String) getTitle.invoke(event.getInventory());
            } else {
                return event.getView().getTitle();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Class<?> getNMSClass(String what) {
        try {
            return Class.forName("net.minecraft.server." + SERVER_VERSION + "." + what);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("NMS class not found: " + what, e);
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object handle     = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass()
                    .getMethod("sendPacket",
                            Class.forName("net.minecraft.server." + SERVER_VERSION + ".Packet"))
                    .invoke(connection, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Constructor<?> getConstructorByParamCount(Class<?> clazz, int count) {
        for (Constructor<?> c : clazz.getConstructors()) {
            if (c.getParameterCount() == count) return c;
        }
        return clazz.getConstructors()[0];
    }

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isPaperBrigadierSupported() {
        try {
            Class.forName("io.papermc.paper.command.brigadier.Commands");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void log(String message) {
        Bukkit.getConsoleSender().sendMessage(CC.translate(message));
    }

    public static void stop() {
        Bukkit.shutdown();
    }
}