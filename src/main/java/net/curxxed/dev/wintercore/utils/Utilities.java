package net.curxxed.dev.wintercore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

    private static final String SERVER_VERSION;
    public static final boolean IS_LEGACY;
    private static Field pingField = null;

    static {
        SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        IS_LEGACY = getMajorVersion() < 17;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    public static double[] getTPS() {
        try {
            try {
                Method paperGetTPS = Bukkit.getServer().getClass().getMethod("getTPS");
                return (double[]) paperGetTPS.invoke(Bukkit.getServer());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {}

            Object minecraftServer;
            if (IS_LEGACY) {
                minecraftServer = Class.forName("net.minecraft.server." + SERVER_VERSION + ".MinecraftServer")
                        .getMethod("getServer").invoke(null);
            } else {
                minecraftServer = Class.forName("net.minecraft.server.MinecraftServer")
                        .getMethod("getServer").invoke(null);
            }

            Field tpsField = minecraftServer.getClass().getField("recentTps");
            return (double[]) tpsField.get(minecraftServer);

        } catch (Exception e) {
           return new double[] {-1,-1,-1};
        }
    }

    public static int getMajorVersion() {
        try {
            return Integer.parseInt(SERVER_VERSION.split("_")[1]);
        } catch (Exception e) {
            return 17;
        }
    }

    /*public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        if (IS_LEGACY) {
            return Class.forName("net.minecraft.server." + SERVER_VERSION + "." + name);
        } else {
            switch (name) {
                case "PacketPlayOutScoreboardTeam":
                    return Class.forName("net.minecraft.network.protocol.game." + name);
                case "ScoreboardTeam":
                    return Class.forName("net.minecraft.world.scores." + name);
                case "Scoreboard":
                    return Class.forName("net.minecraft.world.scores." + name);
                case "Packet":
                    return Class.forName("net.minecraft.network.protocol." + name);
                case "Container":
                    return Class.forName("net.minecraft.world.inventory." + name);
                case "PlayerInventory":
                    return Class.forName("net.minecraft.world.entity.player." + name);
                case "InventorySubcontainer":
                    return Class.forName("net.minecraft.world.inventory." + name);
                case "EnumChatFormat":
                    return Class.forName("net.minecraft." + name);
                case "IChatBaseComponent":
                case "ChatComponentText":
                case "Component":
                    return Class.forName("net.minecraft.network.chat.IChatBaseComponent");
                default:
                    throw new ClassNotFoundException("Unsupported class for modern NMS: " + name);
            }
        }
    }*/ //commented out as not used currently


    public static Class<?> getCraftBukkitClass(String path) throws ClassNotFoundException {
        if (IS_LEGACY) {
            return Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION + "." + path);
        } else {
            return Class.forName("org.bukkit.craftbukkit." + path);
        }
    }

    public static Object getEntityPlayer(Player player) throws Exception {
        Class<?> craftPlayer = getCraftBukkitClass("entity.CraftPlayer");
        Method getHandle = craftPlayer.getMethod("getHandle"); // convert Bukkit Player to EntityPlayer
        return getHandle.invoke(craftPlayer.cast(player));
    }



    public static int getPing(Player player) {
        try {
            Object entityPlayer = getEntityPlayer(player);

            if (IS_LEGACY) {
                if (pingField == null) {
                    pingField = entityPlayer.getClass().getField("ping");
                }
                return pingField.getInt(entityPlayer);
            } else {
                //noinspection JavaReflectionMemberAccess -- modern versions have a getPing() method
                Method getPing = Player.class.getMethod("getPing");
                return (int) getPing.invoke(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static @NotNull List<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getServer().getOnlinePlayers());
    }

    public static void logBootBanner() {
        Bukkit.getLogger().info("Utilities initialized → Version: " + SERVER_VERSION + " | Mode: " + (IS_LEGACY ? "Legacy NMS/CraftBukkit" : "Modern Modular NMS/CraftBukkit"));
    }

    public static String getInventoryTitle(InventoryClickEvent event) {
        try {
            if (IS_LEGACY) {
                Method getTitleMethod = event.getInventory().getClass().getMethod("getTitle");
                return (String) getTitleMethod.invoke(event.getInventory());
            } else {
                return event.getView().getTitle();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void log (String message) {
      Bukkit.getConsoleSender().sendMessage(CC.translate(message));
  }

    public static void stop() {
        Bukkit.shutdown();
    }
}
