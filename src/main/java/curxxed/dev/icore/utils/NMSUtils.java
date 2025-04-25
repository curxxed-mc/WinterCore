package curxxed.dev.icore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NMSUtils {

    private static final String SERVER_VERSION;
    public static final boolean IS_LEGACY;
    private static final Map<Character, String> COLOR_CODE_TO_ENUM_NAME = new HashMap<>();
    private static final Map<Character, String> FORMAT_CODE_TO_ENUM_NAME = new HashMap<>();

    static {
        SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        IS_LEGACY = getMajorVersion() < 17;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    public static double[] getTPS() {
        try {
            if (IS_LEGACY) {
                Object minecraftServer = Class.forName("net.minecraft.server." + SERVER_VERSION + ".MinecraftServer")
                        .getMethod("getServer").invoke(null);
                return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
            } else {
                Object minecraftServer = Class.forName("net.minecraft.server.MinecraftServer")
                        .getMethod("getServer").invoke(null);
                return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new double[]{0.0, 0.0, 0.0};
        }
    }

    public static int getMajorVersion() {
        try {
            return Integer.parseInt(SERVER_VERSION.split("_")[1]);
        } catch (Exception e) {
            return 17;
        }
    }

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        if (IS_LEGACY) {
            return Class.forName("net.minecraft.server." + SERVER_VERSION + "." + name);
        } else {
            switch (name) {
                case "PacketPlayOutScoreboardTeam":
                    return Class.forName("net.minecraft.network.protocol.game." + name);
                case "ScoreboardTeam":
                    return Class.forName("net.minecraft.world.scores." + name);
                case "Scoreboard":
                    return Class.forName("net.minecraft.world.scores." + name); // Add this mapping
                case "Packet":
                    return Class.forName("net.minecraft.network.protocol.Packet");
                case "Container":
                    return Class.forName("net.minecraft.world.inventory." + name);
                case "PlayerInventory":
                    return Class.forName("net.minecraft.world.entity.player." + name);
                case "InventorySubcontainer":
                    return Class.forName("net.minecraft.world.inventory." + name);
                case "EnumChatFormat":
                    return Class.forName("net.minecraft." + name);
                default:
                    throw new ClassNotFoundException("Unsupported class for modern NMS: " + name);
            }
        }
    }

    public static Class<?> getCraftBukkitClass(String path) throws ClassNotFoundException {
        if (IS_LEGACY) {
            return Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION + "." + path);
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
            Object ep = getEntityPlayer(player);
            if (IS_LEGACY) {
                Field pingField = ep.getClass().getField("ping");
                return pingField.getInt(ep);
            } else {
                Method getPing = player.getClass().getMethod("getPing");
                return (int) getPing.invoke(player);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object ep = getEntityPlayer(player);
            Field connectionField = ep.getClass().getField("playerConnection");
            Object connection = connectionField.get(ep);

            Class<?> packetClass = getNMSClass("Packet");
            Method sendPacket = connection.getClass().getMethod("sendPacket", packetClass);
            sendPacket.invoke(connection, packet);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to send packet", e);
        }
    }

    public static Object createInstance(Class<?> clazz, Object... args) throws Exception {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (matchParameters(constructor.getParameterTypes(), args)) {
                return constructor.newInstance(args);
            }
        }
        throw new IllegalArgumentException("No matching constructor for " + clazz.getName());
    }

    private static boolean matchParameters(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) return false;
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] != null && !paramTypes[i].isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    public static void logBootBanner() {
        Bukkit.getLogger().info("NMSUtils initialized → Version: " + SERVER_VERSION + " | Mode: " + (IS_LEGACY ? "Legacy NMS/CraftBukkit" : "Modern Modular NMS/CraftBukkit"));
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


    static {
        COLOR_CODE_TO_ENUM_NAME.put('0', "BLACK");
        COLOR_CODE_TO_ENUM_NAME.put('1', "DARK_BLUE");
        COLOR_CODE_TO_ENUM_NAME.put('2', "DARK_GREEN");
        COLOR_CODE_TO_ENUM_NAME.put('3', "DARK_AQUA");
        COLOR_CODE_TO_ENUM_NAME.put('4', "DARK_RED");
        COLOR_CODE_TO_ENUM_NAME.put('5', "DARK_PURPLE");
        COLOR_CODE_TO_ENUM_NAME.put('6', "GOLD");
        COLOR_CODE_TO_ENUM_NAME.put('7', "GRAY");
        COLOR_CODE_TO_ENUM_NAME.put('8', "DARK_GRAY");
        COLOR_CODE_TO_ENUM_NAME.put('9', "BLUE");
        COLOR_CODE_TO_ENUM_NAME.put('a', "GREEN");
        COLOR_CODE_TO_ENUM_NAME.put('b', "AQUA");
        COLOR_CODE_TO_ENUM_NAME.put('c', "RED");
        COLOR_CODE_TO_ENUM_NAME.put('d', "LIGHT_PURPLE");
        COLOR_CODE_TO_ENUM_NAME.put('e', "YELLOW");
        COLOR_CODE_TO_ENUM_NAME.put('f', "WHITE");

        FORMAT_CODE_TO_ENUM_NAME.put('k', "OBFUSCATED");
        FORMAT_CODE_TO_ENUM_NAME.put('l', "BOLD");
        FORMAT_CODE_TO_ENUM_NAME.put('m', "STRIKETHROUGH");
        FORMAT_CODE_TO_ENUM_NAME.put('n', "UNDERLINE");
        FORMAT_CODE_TO_ENUM_NAME.put('o', "ITALIC");
        FORMAT_CODE_TO_ENUM_NAME.put('r', "RESET");
    }

    public static String getEnumNameFromColorCode(char colorCode) {
        return COLOR_CODE_TO_ENUM_NAME.getOrDefault(colorCode, "WHITE");
    }


    public static String getEnumNameFromFormatCode(char formatCode) {
        return FORMAT_CODE_TO_ENUM_NAME.getOrDefault(formatCode, "RESET");
    }
}
