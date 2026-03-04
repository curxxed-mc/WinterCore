package net.curxxed.dev.wintercore.disguise.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.events.PlayerDisguiseEvent;
import net.curxxed.dev.wintercore.events.PlayerUnDisguiseEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.SkinFetcher;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class DefaultDisguiseHandler extends DisguiseHandler {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<java.util.UUID, String> disguiseRanks = new ConcurrentHashMap<>();
    private final DisguiseRegistry disguiseRegistry;

    // Resolved once at startup — avoids per-call Class.forName overhead
    private final Class<?> gameProfileClass;
    private final Class<?> propertyClass;

    public DefaultDisguiseHandler(final WinterCore plugin, final DisguiseRegistry disguiseRegistry) {
        super(plugin);
        this.disguiseRegistry = disguiseRegistry;
        this.gameProfileClass = resolveAuthlibClass("GameProfile");
        this.propertyClass    = resolveAuthlibClass("properties.Property");
    }

    // -------------------------------------------------------------------------
    // Authlib class resolution
    //   1.7.10  → net.minecraft.util.com.mojang.authlib.*
    //   1.8+    → com.mojang.authlib.*
    // -------------------------------------------------------------------------

    private static Class<?> resolveAuthlibClass(String relative) {
        try { return Class.forName("com.mojang.authlib." + relative); } catch (ClassNotFoundException ignored) {}
        try { return Class.forName("net.minecraft.util.com.mojang.authlib." + relative); } catch (ClassNotFoundException ignored) {}
        throw new RuntimeException("Cannot locate authlib class: " + relative
                + ". Neither com.mojang.authlib nor net.minecraft.util.com.mojang.authlib is on the classpath.");
    }

    // -------------------------------------------------------------------------
    // Authlib reflection helpers
    // -------------------------------------------------------------------------

    private Object getGameProfile(Object entityPlayer) throws Exception {
        return entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);
    }

    private Object getProperties(Object gameProfile) throws Exception {
        return gameProfileClass.getMethod("getProperties").invoke(gameProfile);
    }

    private void clearProperties(Object gameProfile) throws Exception {
        getProperties(gameProfile).getClass().getMethod("clear").invoke(getProperties(gameProfile));
    }

    private void putTextureProperty(Object gameProfile, String value, String signature) throws Exception {
        Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class, String.class);
        Object property = propCtor.newInstance("textures", value, signature);

        Object propertiesMap = getProperties(gameProfile);
        propertiesMap.getClass().getMethod("put", Object.class, Object.class)
                .invoke(propertiesMap, "textures", property);
    }

    private void serializeProperties(Object gameProfile, JsonArray out) throws Exception {
        Object propertiesMap = getProperties(gameProfile);
        Collection<Map.Entry<Object, Object>> entries =
                (Collection<Map.Entry<Object, Object>>) propertiesMap.getClass()
                        .getMethod("entries").invoke(propertiesMap);

        for (Map.Entry<Object, Object> entry : entries) {
            String key  = (String) entry.getKey();
            Object prop = entry.getValue();
            String pName = (String) propertyClass.getMethod("getName").invoke(prop);
            String pVal  = (String) propertyClass.getMethod("getValue").invoke(prop);
            String pSig  = (String) propertyClass.getMethod("getSignature").invoke(prop);

            JsonObject obj = new JsonObject();
            obj.addProperty("key",        key);
            obj.addProperty("value-name", pName);
            obj.addProperty("value",      pVal);
            obj.addProperty("signature",  pSig);
            out.add(obj);
        }
    }

    private void setProfileName(Object gameProfile, String newName) {
        changeField(gameProfile, "name", newName);
    }

    // -------------------------------------------------------------------------
    // PlayerInfo packet helpers — abstracts 1.7 static factory vs 1.8+ enum API
    // -------------------------------------------------------------------------

    /**
     * Sends a PacketPlayOutPlayerInfo to a single observer.
     * <p>
     * 1.7 uses static factory methods (addPlayer / removePlayer) with no enum.
     * 1.8+ uses a constructor that takes EnumPlayerInfoAction and an Iterable.
     *
     * @param action       "ADD" or "REMOVE"
     * @param entityPlayer the NMS EntityPlayer to add/remove
     * @param observer     the Bukkit Player who will receive the packet
     */
    private void sendPlayerInfoPacket(String action, Object entityPlayer, Player observer) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutPlayerInfo");
        Object packet;

        if (Utilities.IS_1_7) {
            // 1.7: static factory methods, no enum class exists at all
            String methodName = action.equals("ADD") ? "addPlayer" : "removePlayer";
            packet = packetClass
                    .getMethod(methodName, getNMSClass("EntityPlayer"))
                    .invoke(null, entityPlayer);
        } else {
            // 1.8+: PacketPlayOutPlayerInfo(EnumPlayerInfoAction, Iterable)
            // EnumPlayerInfoAction is a nested class — must use $ notation
            Class<?> enumClass = doesClassExists("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    ? getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    : getNMSClass("EnumPlayerInfoAction");

            // ADD_PLAYER = index 0, REMOVE_PLAYER = index 4
            Object enumValue = enumClass.getEnumConstants()[action.equals("ADD") ? 0 : 4];
            packet = packetClass
                    .getConstructor(enumClass, Iterable.class)
                    .newInstance(enumValue, Collections.singleton(entityPlayer));
        }

        sendPacket(observer, packet);
    }

    /** Broadcasts a PlayerInfo ADD or REMOVE packet to every online player. */
    private void broadcastPlayerInfoPacket(String action, Object entityPlayer) throws Exception {
        for (Player online : Bukkit.getOnlinePlayers()) {
            sendPlayerInfoPacket(action, entityPlayer, online);
        }
    }

    // -------------------------------------------------------------------------
    // disguise(player, rank, name, skin)
    // -------------------------------------------------------------------------

    @Override
    public DisguiseCallback disguise(final Player player, final String rank, final String name, final String skin) throws Exception {
        if (player == null || !player.isOnline()) return DisguiseCallback.ERROR;

        if (plugin.getRankManager().getRanksSection().getConfigurationSection(rank) == null) {
            return DisguiseCallback.NO_RANK_FOUND;
        }
        Player check = Bukkit.getPlayerExact(name);
        if (check != null && !check.getName().equals(player.getName())) {
            return DisguiseCallback.GLOBAL_PLAYER_FOUND;
        }

        final String version      = Utilities.getServerVersion();
        final boolean flying      = player.isFlying();
        final boolean allowFlight = player.getAllowFlight();
        final Object entityPlayer = Utilities.getEntityPlayer(player);
        final Object gameProfile  = getGameProfile(entityPlayer);

        // ---- Snapshot current profile so we can restore it on undisguise ----
        final JsonObject data = new JsonObject();
        data.addProperty("name", player.getName());
        data.addProperty("uuid", player.getUniqueId().toString());
        final JsonArray properties = new JsonArray();
        serializeProperties(gameProfile, properties);
        data.add("properties", properties);

        final DisguiseData disguiseData = new DisguiseData(rank, name, skin, data, System.currentTimeMillis());
        plugin.getDisguiseDataMap().put(player.getUniqueId(), disguiseData);
        disguiseRanks.put(player.getUniqueId(), rank);

        // ---- Fetch target skin (async) ----
        SkinFetcher.SkinProperty skinProperty = null;
        try {
            CompletableFuture<SkinFetcher.SkinProperty> future =
                    CompletableFuture.supplyAsync(() -> fetchSkinData(skin));
            skinProperty = future.get();
        } catch (Exception ignored) {}

        disguiseRegistry.setDisguised(player, skinProperty);

        // Fallback skin (FaceSlap_) in case fetching fails
        String value     = "ewogICJ0aW1lc3RhbXAiIDogMTU5OTkxNzE1OTc4NiwKICAicHJvZmlsZUlkIiA6ICJhZDI1N2Q0ZmJmZjc0YWRhOTY3ZDM0YWZjM2Q5NTcyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGYWNlU2xhcF8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQzYjA2YzM4NTA0ZmZjMDIyOWI5NDkyMTQ3YzY5ZmNmNTlmZDJlZDc4ODVmNzg1MDIxNTJmNzdiNGQ1MGRlMSIKICAgIH0KICB9Cn0=";
        String signature = "ICq7KLYfdYPI4v3aFxEvpYadhFoYptjKtEhybC4vFnHd081JHiLTuSIqtYPwpqCSkIG+ooUrUMJ/Qka+ieKuOqefmQ+03apVmCeQVnqcYVMyzJTvp69q1Q1TPlc7G/tLgtyF+Ct/E6u/kZ6Dc494VsuXQj6wfLg7+yqqb2Y9PAr2Np91x0AbKithM1vOqvXAcvZRGILp/BAhZ817myXa/CkrvTxFEbiXbD8isWw+tIXLlPi+3Ck5r6KS3tHBGH7/IeY2WM7DN5/vRATfkKGo2F+H6s8IB9t/2bIWG39TKmxYg6wX0daa/FkpEhXb7O61HvhOnpmewKs0b40sK+E5+IC+tx9SlDLsFFeTALjpc2qwOOQ25ITFN4EgdHaP9bO4PGrcIHB7lz7fIRwJSxxHAsxfqc5nzRogy3cXFvsa8pByPGSSdvNzysYN2wGOyIaY+oMXPCfrnGVuno1cJk4L/8noGCX9pLRUd/Ow2WSjTl6zaIfgiEa4d7JWdxdL9/+UQja6oKoQldbMpRTwQPL5uyGbkrirPMNud1s1qaBVrrDUDQoJM0XrYxSF+TtUWRd3kWTN7x7QWdh+8hFECB9H5Kl6k0TyLTSAJkFbKE6aKSLXnSPW7Rb7F/6D3/NRFuDKLDm1exdKBRG3qr0ThB1LhOSE8nOOztETDoPkZJEwWho=";
        if (skinProperty != null) {
            value     = skinProperty.value;
            signature = skinProperty.signature;
        }

        // ---- Swap texture on the profile ----
        clearProperties(gameProfile);
        putTextureProperty(gameProfile, value, signature);

        // ---- Refresh visibility for all players ----
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(online -> online.hidePlayer(player));
            Bukkit.getOnlinePlayers().forEach(online -> online.showPlayer(player));
        });

        // ---- Send PlayerInfo REMOVE ----
        broadcastPlayerInfoPacket("REMOVE", entityPlayer);

        // ---- Send EntityDestroy ----
        final Class<?> packetPlayOutEntityDestroy = getNMSClass("PacketPlayOutEntityDestroy");
        final int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
        final Object packetDestroy = packetPlayOutEntityDestroy.getConstructor(int[].class)
                .newInstance((Object) new int[]{entityId});
        for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, packetDestroy);

        // ---- Rename profile ----
        // 1.7 stores the tab-list name in "listName"; 1.8+ uses "displayName"
        final String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
        setProfileName(gameProfile, name);
        changeField(entityPlayer, displayField, name);

        // ---- Send PlayerInfo ADD ----
        broadcastPlayerInfoPacket("ADD", entityPlayer);

        // ---- Spawn entity ----
        final Class<?> packetPlayOutNamedEntitySpawn = getNMSClass("PacketPlayOutNamedEntitySpawn");
        final Object packetSpawn = getConstructorWithParameterExact(packetPlayOutNamedEntitySpawn, 1)
                .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
        for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, packetSpawn);

        // ---- Equipment packets ----
        // 1.7 and 1.8 both use integer slots (0-3); EnumItemSlot only exists on 1.9+
        if (Utilities.IS_1_7 || version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(i -> {
                try {
                    final Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                    final Object pkt = packetEquip.getConstructor(Integer.TYPE, Integer.TYPE, getNMSClass("ItemStack"))
                            .newInstance(entityId, i,
                                    safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE).invoke(entityPlayer, i),
                                            getNMSClass("ItemStack")));
                    for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, pkt);
                } catch (Exception e) { e.printStackTrace(); }
            });
        } else {
            final Class<?> enumSlotsClass = getNMSClass("EnumItemSlot");
            for (Object constant : enumSlotsClass.getEnumConstants()) {
                final Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                final Object pkt = packetEquip.getConstructor(Integer.TYPE, enumSlotsClass, getNMSClass("ItemStack"))
                        .newInstance(entityId, constant,
                                safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass).invoke(entityPlayer, constant),
                                        getNMSClass("ItemStack")));
                for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, pkt);
            }
        }

        // ---- Respawn self ----
        final int held = player.getInventory().getHeldItemSlot();
        sendPacket(player, packetDestroy);
        Bukkit.getScheduler().runTask(plugin, () -> {
            moveToWorld(player);
            player.setFlying(flying);
            player.setAllowFlight(allowFlight);
            player.getInventory().setHeldItemSlot(held);
            player.updateInventory();
        });

        // ---- Store disguise info in Redis ----
        String rawPrefix = plugin.getRankManager().getRanksSection().getConfigurationSection(rank) != null
                ? plugin.getRankManager().getRanksSection().getConfigurationSection(rank).getString("prefix", "") : "";
        String nameColor = plugin.getRankManager().getRanksSection().getConfigurationSection(rank) != null
                ? plugin.getRankManager().getRanksSection().getConfigurationSection(rank).getString("name-color", "&f") : "&f";
        String coloredPrefix = net.curxxed.dev.wintercore.utils.CC.translate(rawPrefix);

        disguiseRegistry.setDisguiseInfo(player, name, rank, skin, nameColor, coloredPrefix);
        disguiseRegistry.getEffectiveColor(player, color -> {
            if (plugin.getNameTagHandler() != null && plugin.getNameTagHandler().getNameTagAdapter() != null) {
                plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, color);
            }
        });

        updateTabListAndTeam(player, name);
        Bukkit.getPluginManager().callEvent(new PlayerDisguiseEvent(player, player.getName(), name, rank));
        return DisguiseCallback.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // unDisguise(player, save)
    // -------------------------------------------------------------------------

    @Override
    public DisguiseCallback unDisguise(final Player player, final boolean save) throws Exception {
        final DisguiseData disguiseData = plugin.getDisguiseDataMap().get(player.getUniqueId());
        if (disguiseData == null) {
            plugin.getRedisManager().clearDisguise(player.getUniqueId());
            return DisguiseCallback.NOT_DISGUISED;
        }

        final JsonObject profileData  = disguiseData.getInfo();
        final boolean flying          = player.isFlying();
        final boolean allowFlight     = player.getAllowFlight();
        final Object entityPlayer     = Utilities.getEntityPlayer(player);
        final Object gameProfile      = getGameProfile(entityPlayer);

        // ---- Restore original skin from snapshot ----
        if (profileData.has("properties")) {
            final JsonArray propsJson = profileData.get("properties").getAsJsonArray();
            if (propsJson.size() > 0) {
                JsonObject first = propsJson.get(0).getAsJsonObject();
                if (first.has("value") && first.has("signature")) {
                    clearProperties(gameProfile);
                    putTextureProperty(gameProfile,
                            first.get("value").getAsString(),
                            first.get("signature").getAsString());
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getOnlinePlayers().forEach(online -> online.hidePlayer(player));
            Bukkit.getOnlinePlayers().forEach(online -> online.showPlayer(player));
        });

        // ---- Send PlayerInfo REMOVE ----
        broadcastPlayerInfoPacket("REMOVE", entityPlayer);

        // ---- Send EntityDestroy ----
        final Class<?> packetPlayOutEntityDestroy = getNMSClass("PacketPlayOutEntityDestroy");
        final int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
        final Object packetDestroy = packetPlayOutEntityDestroy.getConstructor(int[].class)
                .newInstance((Object) new int[]{entityId});
        for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, packetDestroy);

        // ---- Restore original profile name ----
        final String originalName = profileData.get("name").getAsString();
        // 1.7 stores the tab-list name in "listName"; 1.8+ uses "displayName"
        final String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
        setProfileName(gameProfile, originalName);
        changeField(entityPlayer, displayField, originalName);

        if (!save) {
            Bukkit.getPluginManager().callEvent(
                    new PlayerUnDisguiseEvent(player, disguiseData.getName(), originalName, disguiseData.getRank()));
            if (plugin.getNameTagHandler() != null && plugin.getNameTagHandler().getNameTagAdapter() != null) {
                plugin.getNameTagHandler().getNameTagAdapter().resetNameTag(player);
            }
            plugin.getRedisManager().clearDisguise(player.getUniqueId());
            return DisguiseCallback.SUCCESS;
        }

        // ---- Send PlayerInfo ADD ----
        broadcastPlayerInfoPacket("ADD", entityPlayer);

        // ---- Spawn entity ----
        final Class<?> packetPlayOutNamedEntitySpawn = getNMSClass("PacketPlayOutNamedEntitySpawn");
        final Object packetSpawn = getConstructorWithParameterExact(packetPlayOutNamedEntitySpawn, 1)
                .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
        for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, packetSpawn);

        // ---- Equipment packets ----
        // 1.7 and 1.8 both use integer slots (0-3); EnumItemSlot only exists on 1.9+
        final String version = Utilities.getServerVersion();
        if (Utilities.IS_1_7 || version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(i -> {
                try {
                    final Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                    final Object pkt = packetEquip.getConstructor(Integer.TYPE, Integer.TYPE, getNMSClass("ItemStack"))
                            .newInstance(entityId, i,
                                    safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE).invoke(entityPlayer, i),
                                            getNMSClass("ItemStack")));
                    for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, pkt);
                } catch (Exception e) { e.printStackTrace(); }
            });
        } else {
            final Class<?> enumSlotsClass = getNMSClass("EnumItemSlot");
            for (Object constant : enumSlotsClass.getEnumConstants()) {
                final Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                final Object pkt = packetEquip.getConstructor(Integer.TYPE, enumSlotsClass, getNMSClass("ItemStack"))
                        .newInstance(entityId, constant,
                                safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass).invoke(entityPlayer, constant),
                                        getNMSClass("ItemStack")));
                for (Player online : Bukkit.getOnlinePlayers()) sendPacket(online, pkt);
            }
        }

        // ---- Respawn self ----
        final int held = player.getInventory().getHeldItemSlot();
        sendPacket(player, packetDestroy);
        Bukkit.getScheduler().runTask(plugin, () -> {
            moveToWorld(player);
            player.setFlying(flying);
            player.setAllowFlight(allowFlight);
            player.getInventory().setHeldItemSlot(held);
            player.updateInventory();
        });

        Bukkit.getPluginManager().callEvent(
                new PlayerUnDisguiseEvent(player, disguiseData.getName(), originalName, disguiseData.getRank()));

        plugin.getDisguiseDataMap().remove(player.getUniqueId());
        disguiseRanks.remove(player.getUniqueId());
        disguiseRegistry.clear(player);
        plugin.getRankManager().refreshPlayerDisplay(player);
        plugin.getRedisManager().clearDisguise(player.getUniqueId());

        if (plugin.getNameTagHandler().getNameTagAdapter() != null) {
            plugin.getNameTagHandler().getNameTagAdapter().resetNameTag(player);
        } else {
            System.out.println("[WinterCore] NameTagAdapter is null! Cannot reset name tag for " + player.getName());
        }
        return DisguiseCallback.SUCCESS;
    }

    // -------------------------------------------------------------------------
    // disguise(player, targetName) — simple skin-only disguise
    // -------------------------------------------------------------------------

    @Override
    public DisguiseCallback disguise(Player player, String targetName) throws Exception {
        String version = Utilities.getServerVersion();
        if (!(version.startsWith("v1_7") || version.startsWith("v1_8"))) return DisguiseCallback.ERROR;
        if (player == null || !player.isOnline()) return DisguiseCallback.NOT_ONLINE;
        if (targetName.equalsIgnoreCase(player.getName()))  return DisguiseCallback.SAME_NAME;

        Player check = Bukkit.getPlayerExact(targetName);
        if (check != null && !check.getName().equals(player.getName())) return DisguiseCallback.GLOBAL_PLAYER_FOUND;

        SkinFetcher.SkinProperty skinProperty = fetchSkinData(targetName);
        if (skinProperty == null) return DisguiseCallback.ERROR;

        // Snapshot original profile if not already done
        if (!plugin.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            Object entityPlayer = Utilities.getEntityPlayer(player);
            Object gameProfile  = getGameProfile(entityPlayer);
            JsonObject data     = new JsonObject();
            data.addProperty("name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            JsonArray props = new JsonArray();
            serializeProperties(gameProfile, props);
            data.add("properties", props);
            plugin.getDisguiseDataMap().put(player.getUniqueId(),
                    new DisguiseData("", player.getName(), player.getName(), data, System.currentTimeMillis()));
        }
        return disguise(player, "", targetName, targetName);
    }

    // -------------------------------------------------------------------------
    // undisguise(player)
    // -------------------------------------------------------------------------

    @Override
    public DisguiseCallback undisguise(Player player) throws Exception {
        String version = Utilities.getServerVersion();
        if (!(version.startsWith("v1_7") || version.startsWith("v1_8"))) return DisguiseCallback.ERROR;
        if (player == null || !player.isOnline()) return DisguiseCallback.NOT_ONLINE;
        return unDisguise(player, true);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void updateTabListAndTeam(Player player, String name) {
        disguiseRegistry.getEffectiveColor(player, color -> {
            if (!player.isOnline()) return;
            if (plugin.getNameTagHandler() != null && plugin.getNameTagHandler().getNameTagAdapter() != null) {
                plugin.getNameTagHandler().getNameTagAdapter().setNameTag(player, color);
            }
        });
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getNameTagHandler() != null && plugin.getNameTagHandler().getNameTagAdapter() != null) {
            plugin.getNameTagHandler().getNameTagAdapter().resetNameTag(player);
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,
                () -> plugin.getRedisManager().clearDisguise(player.getUniqueId()), 100L);
    }
}