package net.curxxed.dev.wintercore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utilities {

    private static final String SERVER_VERSION;
    public static final boolean IS_LEGACY;
    private static final Map<Character, String> COLOR_CODE_TO_ENUM_NAME = new HashMap<>();
    private static final Map<Character, String> FORMAT_CODE_TO_ENUM_NAME = new HashMap<>();
    private static Method getPingMethod = null;
    private static Field pingField = null;
    public static final String PING_ERROR = CC.translate("&cError while getting ping, either your version is not supported or there is an exception!");

    static {
        SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        IS_LEGACY = getMajorVersion() < 17;
    }

    public static String getServerVersion() {
        return SERVER_VERSION;
    }

    public static double[] getTPS() {
        try {
            Object minecraftServer;
            if (IS_LEGACY) {
                minecraftServer = Class.forName("net.minecraft.server." + SERVER_VERSION + ".MinecraftServer")
                        .getMethod("getServer").invoke(null);
            } else {
                minecraftServer = Class.forName("net.minecraft.server.MinecraftServer")
                        .getMethod("getServer").invoke(null);
            }
            return (double[]) minecraftServer.getClass().getField("recentTps").get(minecraftServer);
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
            Object entityPlayer = getEntityPlayer(player);

            if (IS_LEGACY) {
                if (pingField == null) {
                    pingField = entityPlayer.getClass().getField("ping");
                }
                return pingField.getInt(entityPlayer);
            } else {
                if (getPingMethod == null) {
                    try {
                        getPingMethod = entityPlayer.getClass().getMethod("e_");
                    } catch (NoSuchMethodException e) {
                        try {
                            getPingMethod = entityPlayer.getClass().getMethod("getLatency");
                        } catch (NoSuchMethodException ex) {
                            getPingMethod = entityPlayer.getClass().getMethod("getPing");
                        }
                    }
                }
                return (int) getPingMethod.invoke(entityPlayer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static @NotNull List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            players.add(player);
        }
        return players;
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

    public static void stopServerSmart() {
        // Set shutdown flag
        net.curxxed.dev.wintercore.plugin.WinterCore.isShuttingDown = true;

        String nmsPackage = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        Bukkit.getLogger().info("[WinterCore] stopServerSmart() called. Attempting shutdown...");
        boolean instanceStopAttempted = false;
        try {
            boolean isPaper = false;
            try { Class.forName("co.aikar.timings.Timings"); isPaper = true; } catch (ClassNotFoundException ignored) {}
            Class<?> mcServerClass = Class.forName("net.minecraft.server." + nmsPackage + ".MinecraftServer");
            if (isPaper) {
                try {
                    Bukkit.getLogger().info("[WinterCore] Detected PaperSpigot or fork. Trying MinecraftServer.stopServer()...");
                    mcServerClass.getMethod("stopServer").invoke(null);
                    Bukkit.getLogger().info("[WinterCore] Called MinecraftServer.stopServer() (Paper) - but will also call instance stop() as fallback.");
                } catch (NoSuchMethodException e) {
                    Bukkit.getLogger().warning("[WinterCore] MinecraftServer.stopServer() not found. Trying SpigotTimings.stopServer()...");
                    try {
                        Class<?> timingsClass = Class.forName("co.aikar.timings.SpigotTimings");
                        timingsClass.getMethod("stopServer").invoke(null);
                        Bukkit.getLogger().info("[WinterCore] Called SpigotTimings.stopServer() - but will also call instance stop() as fallback.");
                    } catch (Exception e2) {
                        Bukkit.getLogger().severe("[WinterCore] Exception calling SpigotTimings.stopServer(): " + e2.getMessage());
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().severe("[WinterCore] Exception calling MinecraftServer.stopServer(): " + e.getMessage());
                }
            }
            // Always call instance stop() as a final fallback
            try {
                Bukkit.getLogger().info("[WinterCore] Calling MinecraftServer.getServer().stop() (instance method) as final fallback.");
                Object server = mcServerClass.getMethod("getServer").invoke(null);
                mcServerClass.getMethod("stop").invoke(server);
                Bukkit.getLogger().info("[WinterCore] Called MinecraftServer.getServer().stop() successfully.");
                Bukkit.shutdown();
                instanceStopAttempted = true;
            } catch (Exception e) {
                Bukkit.getLogger().severe("[WinterCore] Exception calling instance stop(): " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[WinterCore] Exception in stopServerSmart(): " + e.getMessage());
            e.printStackTrace();
        }
        // As a last resort, force exit if nothing worked
        if (!instanceStopAttempted) {
            Bukkit.getLogger().severe("[WinterCore] Server did not shut down as expected. Forcing System.exit(0)");
            System.exit(0);
        }
    }
}
