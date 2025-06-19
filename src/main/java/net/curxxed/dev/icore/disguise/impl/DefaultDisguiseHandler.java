package net.curxxed.dev.icore.disguise.impl;

import net.curxxed.dev.icore.disguise.DisguiseHandler;
import net.curxxed.dev.icore.plugin.iCore;
import org.bukkit.entity.Player;
import net.curxxed.dev.icore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.icore.utils.NMSUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.curxxed.dev.icore.disguise.player.DisguiseData;
import net.curxxed.dev.icore.utils.SkinFetcher;
import org.bukkit.Bukkit;
import net.curxxed.dev.icore.events.PlayerDisguiseEvent;
import net.curxxed.dev.icore.events.PlayerUnDisguiseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import net.curxxed.dev.icore.disguise.DisguiseRegistry;
import net.curxxed.dev.icore.utils.CC;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DefaultDisguiseHandler extends DisguiseHandler {

    // Map to store disguise ranks separately from real ranks
    private final Map<java.util.UUID, String> disguiseRanks = new ConcurrentHashMap<>();
    private final DisguiseRegistry disguiseRegistry;

    public DefaultDisguiseHandler(final iCore plugin, final DisguiseRegistry disguiseRegistry) {
        super(plugin);
        this.disguiseRegistry = disguiseRegistry;
    }

    @Override
    public DisguiseCallback disguise(final Player player, final String rank, final String name, final String skin) throws Exception {
        if (player == null || !player.isOnline()) {
            return DisguiseCallback.ERROR;
        }
        Player check = Bukkit.getPlayerExact(name);
        if (check != null && !check.getName().equals(player.getName())) {
            return DisguiseCallback.GLOBAL_PLAYER_FOUND;
        }
        final String version = NMSUtils.getServerVersion();
        final boolean flying = player.isFlying();
        final boolean allowFlight = player.getAllowFlight();
        final Object entityPlayer = NMSUtils.getEntityPlayer(player);
        final GameProfile gameProfile = (GameProfile) entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);

        // Save current skin/profile data
        final JsonObject data = new JsonObject();
        data.addProperty("name", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        final JsonArray properties = new JsonArray();
        gameProfile.getProperties().entries().forEach(entry -> {
            final JsonObject object = new JsonObject();
            object.addProperty("key", entry.getKey());
            object.addProperty("value-name", entry.getValue().getName());
            object.addProperty("value", entry.getValue().getValue());
            object.addProperty("signature", entry.getValue().getSignature());
            properties.add(object);
        });
        data.add("properties", properties);

        final DisguiseData disguiseData = new DisguiseData(rank, name, skin, data, System.currentTimeMillis());
        plugin.getDisguiseDataMap().put(player.getUniqueId(), disguiseData);
        // Store disguise rank in the map
        disguiseRanks.put(player.getUniqueId(), rank);

        // Fetch skin property (async/await style)
        SkinFetcher.SkinProperty skinProperty = null;
        try {
            java.util.concurrent.CompletableFuture<SkinFetcher.SkinProperty> future = java.util.concurrent.CompletableFuture.supplyAsync(() -> fetchSkinData(skin));
            skinProperty = future.get();
        } catch (Exception e) {
            // fallback to default below
        }
        String value = "ewogICJ0aW1lc3RhbXAiIDogMTU5OTkxNzE1OTc4NiwKICAicHJvZmlsZUlkIiA6ICJhZDI1N2Q0ZmJmZjc0YWRhOTY3ZDM0YWZjM2Q5NTcyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGYWNlU2xhcF8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQzYjA2YzM4NTA0ZmZjMDIyOWI5NDkyMTQ3YzY5ZmNmNTlmZDJlZDc4ODVmNzg1MDIxNTJmNzdiNGQ1MGRlMSIKICAgIH0KICB9Cn0=";
        String signature = "ICq7KLYfdYPI4v3aFxEvpYadhFoYptjKtEhybC4vFnHd081JHiLTuSIqtYPwpqCSkIG+ooUrUMJ/Qka+ieKuOqefmQ+03apVmCeQVnqcYVMyzJTvp69q1Q1TPlc7G/tLgtyF+Ct/E6u/kZ6Dc494VsuXQj6wfLg7+yqqb2Y9PAr2Np91x0AbKithM1vOqvXAcvZRGILp/BAhZ817myXa/CkrvTxFEbiXbD8isWw+tIXLlPi+3Ck5r6KS3tHBGH7/IeY2WM7DN5/vRATfkKGo2F+H6s8IB9t/2bIWG39TKmxYg6wX0daa/FkpEhXb7O61HvhOnpmewKs0b40sK+E5+IC+tx9SlDLsFFeTALjpc2qwOOQ25ITFN4EgdHaP9bO4PGrcIHB7lz7fIRwJSxxHAsxfqc5nzRogy3cXFvsa8pByPGSSdvNzysYN2wGOyIaY+oMXPCfrnGVuno1cJk4L/8noGCX9pLRUd/Ow2WSjTl6zaIfgiEa4d7JWdxdL9/+UQja6oKoQldbMpRTwQPL5uyGbkrirPMNud1s1qaBVrrDUDQoJM0XrYxSF+TtUWRd3kWTN7x7QWdh+8hFECB9H5Kl6k0TyLTSAJkFbKE6aKSLXnSPW7Rb7F/6D3/NRFuDKLDm1exdKBRG3qr0ThB1LhOSE8nOOztETDoPkZJEwWho=";
        if (skinProperty != null) {
            value = skinProperty.value;
            signature = skinProperty.signature;
        }
        gameProfile.getProperties().clear();
        gameProfile.getProperties().put("textures", new Property("textures", value, signature));

        // Remove player from server's player list and tablist under old name
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(online -> {
                online.hidePlayer(player);
            });
            Bukkit.getOnlinePlayers().forEach(online -> {
                online.showPlayer(player);
            });
        });
        // Remove and re-add player to update skin
        final Class<?> packetPlayOutPlayerInfo = getNMSClass("PacketPlayOutPlayerInfo");
        final Class<?> enumPlayerInfoAction = doesClassExists("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                ? getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                : getNMSClass("EnumPlayerInfoAction");
        final Constructor<?> constructor = packetPlayOutPlayerInfo.getConstructor(enumPlayerInfoAction, Iterable.class);
        final Object removePlayerEnum = enumPlayerInfoAction.getEnumConstants()[4];
        final Object packetPlayOutPlayerInfoRemoveInitialized = constructor.newInstance(removePlayerEnum, Collections.singleton(entityPlayer));
        for (final Player online : Bukkit.getOnlinePlayers()) {
            sendPacket(online, packetPlayOutPlayerInfoRemoveInitialized);
        }
        final Class<?> packetPlayOutEntityDestroy = getNMSClass("PacketPlayOutEntityDestroy");
        final Object packetPlayOutEntityDestroyInitialized = packetPlayOutEntityDestroy.getConstructor(int[].class)
                .newInstance((Object) new int[]{(int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer)});
        for (final Player online2 : Bukkit.getOnlinePlayers()) {
            sendPacket(online2, packetPlayOutEntityDestroyInitialized);
        }
        changeField(gameProfile, "name", name);
        changeField(entityPlayer, "displayName", name);
        final Object addPlayerEnum = enumPlayerInfoAction.getEnumConstants()[0];
        final Object packetPlayOutPlayerInfoAddInitialized = constructor.newInstance(addPlayerEnum, Collections.singleton(entityPlayer));
        for (final Player online3 : Bukkit.getOnlinePlayers()) {
            sendPacket(online3, packetPlayOutPlayerInfoAddInitialized);
        }
        final Class<?> packetPlayOutNamedEntitySpawn = getNMSClass("PacketPlayOutNamedEntitySpawn");
        final Object packetPlayOutNamedEntitySpawnInitialized = getConstructorWithParameterExact(packetPlayOutNamedEntitySpawn, 1)
                .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
        for (final Player online4 : Bukkit.getOnlinePlayers()) {
            sendPacket(online4, packetPlayOutNamedEntitySpawnInitialized);
        }
        if (version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(i -> {
                try {
                    final Class<?> packetPlayOutEntityEquipment2 = getNMSClass("PacketPlayOutEntityEquipment");
                    final Object packetPlayOutEntityEquipmentInitialized2 = packetPlayOutEntityEquipment2.getConstructor(Integer.TYPE, Integer.TYPE, getNMSClass("ItemStack"))
                            .newInstance((int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer), i, safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE).invoke(entityPlayer, i), getNMSClass("ItemStack")));
                    for (final Player online6 : Bukkit.getOnlinePlayers()) {
                        sendPacket(online6, packetPlayOutEntityEquipmentInitialized2);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            final Class<?> enumSlotsClass = getNMSClass("EnumItemSlot");
            for (final Object constant : enumSlotsClass.getEnumConstants()) {
                final Class<?> packetPlayOutEntityEquipment = getNMSClass("PacketPlayOutEntityEquipment");
                final Object packetPlayOutEntityEquipmentInitialized = packetPlayOutEntityEquipment.getConstructor(Integer.TYPE, enumSlotsClass, getNMSClass("ItemStack"))
                        .newInstance((int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer), constant, safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass).invoke(entityPlayer, constant), getNMSClass("ItemStack")));
                for (final Player online5 : Bukkit.getOnlinePlayers()) {
                    sendPacket(online5, packetPlayOutEntityEquipmentInitialized);
                }
            }
        }
        final int held = player.getInventory().getHeldItemSlot();
        sendPacket(player, packetPlayOutEntityDestroyInitialized);
        Bukkit.getScheduler().runTask(plugin, () -> {
            moveToWorld(player);
            player.setFlying(flying);
            player.setAllowFlight(allowFlight);
            player.getInventory().setHeldItemSlot(held);
            player.updateInventory();
        });
        // Persist disguise in Redis for cross-server skin/name/rank
        String rawPrefix = plugin.getRankManager().getRanksSection().getConfigurationSection(rank) != null ?
                plugin.getRankManager().getRanksSection().getConfigurationSection(rank).getString("prefix", "") : "";
        char colorChar = extractFirstColorChar(plugin.getRankManager().getRanksSection().getConfigurationSection(rank) != null ?
                plugin.getRankManager().getRanksSection().getConfigurationSection(rank).getString("name-color", "&f") : "&f");
        String colorCode = "&" + colorChar;
        // Store disguise info in DisguiseRegistry (and Redis)
        disguiseRegistry.setDisguiseInfo(player, name, rank, colorCode, rawPrefix);
        // Immediately set the name tag color based on the chosen rank using DisguiseRegistry
        disguiseRegistry.getEffectiveColor(player, color -> {
            ChatColor chatColor = ChatColor.WHITE;
            if (color != null && !color.isEmpty()) {
                String translated = net.curxxed.dev.icore.utils.CC.translate(color);
                ChatColor maybe = null;
                if (translated.length() > 1) {
                    char code = translated.charAt(1);
                    maybe = ChatColor.getByChar(code);
                }
                if (maybe != null) chatColor = maybe;
            }
            if (plugin.getNameTagAdapter() != null) {
                plugin.getNameTagAdapter().setNameTag(player, "", chatColor);
            }
        });
        updateTabListAndTeam(player, name);
        com.google.gson.JsonObject disguiseJson = new com.google.gson.JsonObject();
        disguiseJson.addProperty("name", name);
        disguiseJson.addProperty("rank", rank);
        disguiseJson.addProperty("skinValue", value);
        disguiseJson.addProperty("skinSignature", signature);
        disguiseJson.addProperty("color", colorCode);
        // Async Redis call
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getRedisManager().setDisguise(player.getUniqueId(), disguiseJson.toString()));
        PlayerDisguiseEvent event = new PlayerDisguiseEvent(player, player.getName(), name, rank);
        Bukkit.getPluginManager().callEvent(event);
        return DisguiseCallback.SUCCESS;
    }

    @Override
    public DisguiseCallback unDisguise(final Player player, final boolean save) throws Exception {
        final DisguiseData disguiseData = plugin.getDisguiseDataMap().get(player.getUniqueId());
        if (disguiseData == null) {
            plugin.getNameTagAdapter().resetNameTag(player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
            return DisguiseCallback.NOT_DISGUISED;
        }
        final JsonObject profileData = disguiseData.getInfo();
        final boolean flying = player.isFlying();
        final boolean allowFlight = player.getAllowFlight();
        final Object entityPlayer = NMSUtils.getEntityPlayer(player);
        final GameProfile gameProfile = (GameProfile) entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);

        // Restore original skin (if available)
        String value = null, signature = null;
        if (profileData.has("properties")) {
            final JsonArray properties = profileData.get("properties").getAsJsonArray();
            if (properties.size() > 0 && properties.get(0).getAsJsonObject().has("value") && properties.get(0).getAsJsonObject().has("signature")) {
                value = properties.get(0).getAsJsonObject().get("value").getAsString();
                signature = properties.get(0).getAsJsonObject().get("signature").getAsString();
            }
        }
        if (value != null && signature != null) {
            gameProfile.getProperties().clear();
            gameProfile.getProperties().put("textures", new Property("textures", value, signature));
        }

        // Remove disguised player from server's player list and tablist under disguise name
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(online -> {
                online.hidePlayer(player);
            });
            Bukkit.getOnlinePlayers().forEach(online -> {
                online.showPlayer(player);
            });
        });
        // Remove and re-add player to update skin
        final Class<?> packetPlayOutPlayerInfo = getNMSClass("PacketPlayOutPlayerInfo");
        final Class<?> enumPlayerInfoAction = doesClassExists("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                ? getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                : getNMSClass("EnumPlayerInfoAction");
        final Constructor<?> constructor = packetPlayOutPlayerInfo.getConstructor(enumPlayerInfoAction, Iterable.class);
        final Object removePlayerEnum = enumPlayerInfoAction.getEnumConstants()[4];
        final Object packetPlayOutPlayerInfoRemoveInitialized = constructor.newInstance(removePlayerEnum, Collections.singleton(entityPlayer));
        for (final Player online : Bukkit.getOnlinePlayers()) {
            sendPacket(online, packetPlayOutPlayerInfoRemoveInitialized);
        }
        final Class<?> packetPlayOutEntityDestroy = getNMSClass("PacketPlayOutEntityDestroy");
        final Object packetPlayOutEntityDestroyInitialized = packetPlayOutEntityDestroy.getConstructor(int[].class)
                .newInstance((Object) new int[]{(int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer)});
        for (final Player online2 : Bukkit.getOnlinePlayers()) {
            sendPacket(online2, packetPlayOutEntityDestroyInitialized);
        }
        final String name = profileData.get("name").getAsString();
        changeField(gameProfile, "name", name);
        changeField(entityPlayer, "displayName", name);

        if (!save) {
            PlayerUnDisguiseEvent event = new PlayerUnDisguiseEvent(player, disguiseData.getName(), name, disguiseData.getRank());
            Bukkit.getPluginManager().callEvent(event);
            plugin.getNameTagAdapter().resetNameTag(player);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
            return DisguiseCallback.SUCCESS;
        }

        final Object addPlayerEnum = enumPlayerInfoAction.getEnumConstants()[0];
        final Object packetPlayOutPlayerInfoAddInitialized = constructor.newInstance(addPlayerEnum, Collections.singleton(entityPlayer));
        for (final Player online3 : Bukkit.getOnlinePlayers()) {
            sendPacket(online3, packetPlayOutPlayerInfoAddInitialized);
        }
        final Class<?> packetPlayOutNamedEntitySpawn = getNMSClass("PacketPlayOutNamedEntitySpawn");
        final Object packetPlayOutNamedEntitySpawnInitialized = getConstructorWithParameterExact(packetPlayOutNamedEntitySpawn, 1)
                .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
        for (final Player online4 : Bukkit.getOnlinePlayers()) {
            sendPacket(online4, packetPlayOutNamedEntitySpawnInitialized);
        }
        final String version = NMSUtils.getServerVersion();
        if (version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(i -> {
                try {
                    final Class<?> packetPlayOutEntityEquipment2 = getNMSClass("PacketPlayOutEntityEquipment");
                    final Object packetPlayOutEntityEquipmentInitialized2 = packetPlayOutEntityEquipment2.getConstructor(Integer.TYPE, Integer.TYPE, getNMSClass("ItemStack"))
                            .newInstance((int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer), i, safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE).invoke(entityPlayer, i), getNMSClass("ItemStack")));
                    for (final Player online6 : Bukkit.getOnlinePlayers()) {
                        sendPacket(online6, packetPlayOutEntityEquipmentInitialized2);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            final Class<?> enumSlotsClass = getNMSClass("EnumItemSlot");
            for (final Object constant : enumSlotsClass.getEnumConstants()) {
                final Class<?> packetPlayOutEntityEquipment = getNMSClass("PacketPlayOutEntityEquipment");
                final Object packetPlayOutEntityEquipmentInitialized = packetPlayOutEntityEquipment.getConstructor(Integer.TYPE, enumSlotsClass, getNMSClass("ItemStack"))
                        .newInstance((int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer), constant, safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass).invoke(entityPlayer, constant), getNMSClass("ItemStack")));
                for (final Player online5 : Bukkit.getOnlinePlayers()) {
                    sendPacket(online5, packetPlayOutEntityEquipmentInitialized);
                }
            }
        }
        final int held = player.getInventory().getHeldItemSlot();
        sendPacket(player, packetPlayOutEntityDestroyInitialized);
        Bukkit.getScheduler().runTask(plugin, () -> {
            moveToWorld(player);
            player.setFlying(flying);
            player.setAllowFlight(allowFlight);
            player.getInventory().setHeldItemSlot(held);
            player.updateInventory();
        });
        PlayerUnDisguiseEvent event2 = new PlayerUnDisguiseEvent(player, disguiseData.getName(), name, disguiseData.getRank());
        Bukkit.getPluginManager().callEvent(event2);
        plugin.getDisguiseDataMap().remove(player.getUniqueId());
        // Remove disguise rank from map
        disguiseRanks.remove(player.getUniqueId());
        // Schedule Redis disguise cleanup after 5 seconds
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
        if (plugin.getNameTagAdapter() != null) {
            plugin.getNameTagAdapter().resetNameTag(player);
        } else {
            System.out.println("[iCore] NameTagAdapter is null! Cannot reset name tag for " + player.getName());
        }
        return DisguiseCallback.SUCCESS;
    }

    @Override
    public DisguiseCallback disguise(Player player, String targetName) throws Exception {
        // Only allow on 1.7.x and 1.8.x
        String version = NMSUtils.getServerVersion();
        if (!(version.startsWith("v1_7") || version.startsWith("v1_8"))) {
            return DisguiseCallback.ERROR;
        }
        if (player == null || !player.isOnline()) {
            return DisguiseCallback.NOT_ONLINE;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            return DisguiseCallback.SAME_NAME;
        }
        Player check = Bukkit.getPlayerExact(targetName);
        if (check != null && !check.getName().equals(player.getName())) {
            return DisguiseCallback.GLOBAL_PLAYER_FOUND;
        }
        // Fetch target skin
        SkinFetcher.SkinProperty skinProperty = fetchSkinData(targetName);
        if (skinProperty == null) {
            return DisguiseCallback.ERROR;
        }
        // Cache original skin if not already disguised
        if (!plugin.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            Object entityPlayer = NMSUtils.getEntityPlayer(player);
            com.mojang.authlib.GameProfile gameProfile = (com.mojang.authlib.GameProfile) entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);
            com.google.gson.JsonObject data = new com.google.gson.JsonObject();
            data.addProperty("name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            com.google.gson.JsonArray properties = new com.google.gson.JsonArray();
            gameProfile.getProperties().entries().forEach(entry -> {
                com.google.gson.JsonObject object = new com.google.gson.JsonObject();
                object.addProperty("key", entry.getKey());
                object.addProperty("value-name", entry.getValue().getName());
                object.addProperty("value", entry.getValue().getValue());
                object.addProperty("signature", entry.getValue().getSignature());
                properties.add(object);
            });
            data.add("properties", properties);
            plugin.getDisguiseDataMap().put(player.getUniqueId(), new net.curxxed.dev.icore.disguise.player.DisguiseData("", player.getName(), player.getName(), data, System.currentTimeMillis()));
        }
        // Actually disguise
        return disguise(player, "", targetName, targetName);
    }

    @Override
    public DisguiseCallback undisguise(Player player) throws Exception {
        String version = NMSUtils.getServerVersion();
        if (!(version.startsWith("v1_7") || version.startsWith("v1_8"))) {
            return DisguiseCallback.ERROR;
        }
        if (player == null || !player.isOnline()) {
            return DisguiseCallback.NOT_ONLINE;
        }
        return unDisguise(player, true);
    }

    // Helper: Extract only the first color code character from a color string (e.g., "&4&o" -> '4')
    private char extractFirstColorChar(String input) {
        if (input == null) return 'f'; // default to white
        for (int i = 0; i < input.length() - 1; i++) {
            char c = input.charAt(i);
            char code = input.charAt(i + 1);
            if ((c == '§' || c == '&') && ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || (code >= 'A' && code <= 'F'))) {
                return code;
            }
        }
        return 'f'; // default to white
    }

    // Use disguise rank for display if present, else fallback to real rank
    private void updateTabListAndTeam(Player player, String name) {
        disguiseRegistry.getEffectiveColor(player, color -> {
            if (player == null || !player.isOnline()) return;
            char colorChar = extractFirstColorChar(color);
            ChatColor chatColor = ChatColor.getByChar(colorChar);
            if (chatColor == null) chatColor = ChatColor.WHITE;
            if (plugin.getNameTagAdapter() != null) {
                plugin.getNameTagAdapter().setNameTag(player, "", chatColor);
            }
        });
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getNameTagAdapter().resetNameTag(player);
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
    }
}
