package net.curxxed.dev.wintercore.disguise.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.events.disguise.PlayerDisguiseEvent;
import net.curxxed.dev.wintercore.events.disguise.PlayerUnDisguiseEvent;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DefaultDisguiseHandler extends DisguiseHandler {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<UUID, String> disguiseRanks = new ConcurrentHashMap<>();
    private final DisguiseRegistry disguiseRegistry;

    private final Class<?> gameProfileClass;
    private final Class<?> propertyClass;

    public DefaultDisguiseHandler(final WinterCore plugin, final DisguiseRegistry disguiseRegistry) {
        super(plugin);
        this.disguiseRegistry = disguiseRegistry;
        this.gameProfileClass = Utilities.resolveAuthlibClass("GameProfile");
        this.propertyClass = Utilities.resolveAuthlibClass("properties.Property");
    }

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
        propertiesMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertiesMap, "textures", property);
    }

    private void serializeProperties(Object gameProfile, JsonArray out) throws Exception {
        Object propertiesMap = getProperties(gameProfile);
        Collection<Map.Entry<Object, Object>> entries =
                (Collection<Map.Entry<Object, Object>>) propertiesMap.getClass().getMethod("entries").invoke(propertiesMap);

        for (Map.Entry<Object, Object> entry : entries) {
            Object prop = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("key", (String) entry.getKey());
            obj.addProperty("value-name", (String) propertyClass.getMethod("getName").invoke(prop));
            obj.addProperty("value", (String) propertyClass.getMethod("getValue").invoke(prop));
            obj.addProperty("signature", (String) propertyClass.getMethod("getSignature").invoke(prop));
            out.add(obj);
        }
    }

    private void setProfileName(Object gameProfile, String newName) {
        changeField(gameProfile, "name", newName);
    }

    private void setTablistName(Player player, String name) {
        try {
            player.setDisplayName(name);
        } catch (Throwable ignored) {
        }
    }

    private void sendPlayerInfoPacket(String action, Object entityPlayer, Player observer) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutPlayerInfo");
        Object packet;

        if (Utilities.IS_1_7) {
            String methodName = action.equals("ADD") ? "addPlayer" : "removePlayer";
            packet = packetClass.getMethod(methodName, getNMSClass("EntityPlayer")).invoke(null, entityPlayer);
        } else {
            Class<?> enumClass = doesClassExists("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    ? getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    : getNMSClass("EnumPlayerInfoAction");
            Object enumValue = enumClass.getEnumConstants()[action.equals("ADD") ? 0 : 4];
            packet = packetClass.getConstructor(enumClass, Iterable.class).newInstance(enumValue, Collections.singleton(entityPlayer));
        }

        sendPacket(observer, packet);
    }

    private void broadcastPlayerInfoPacket(String action, Object entityPlayer) throws Exception {
        for (Player online : Bukkit.getOnlinePlayers()) {
            sendPlayerInfoPacket(action, entityPlayer, online);
        }
    }

    @Override
    public void disguise(final Player player, final String rank, final String name, final String skin, Consumer<DisguiseCallback> callback) {
        if (player == null || !player.isOnline()) {
            callback.accept(DisguiseCallback.ERROR);
            return;
        }

        if (plugin.getRankManager().getRanksSection().getConfigurationSection(rank) == null) {
            callback.accept(DisguiseCallback.NO_RANK_FOUND);
            return;
        }

        if (isDisguiseNameTakenOnNetwork(name, player.getUniqueId())) {
            callback.accept(DisguiseCallback.GLOBAL_PLAYER_FOUND);
            return;
        }

        final boolean flying = player.isFlying();
        final boolean allowFlight = player.getAllowFlight();

        try {
            final Object entityPlayer = Utilities.getEntityPlayer(player);
            final Object gameProfile = getGameProfile(entityPlayer);

            final JsonObject data = new JsonObject();
            data.addProperty("name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            final JsonArray properties = new JsonArray();
            serializeProperties(gameProfile, properties);
            data.add("properties", properties);

            final DisguiseData disguiseData = new DisguiseData(rank, name, skin, data, System.currentTimeMillis());
            plugin.getDisguiseDataMap().put(player.getUniqueId(), disguiseData);
            disguiseRanks.put(player.getUniqueId(), rank);

            fetchSkinData(skin, skinProperty -> {
                if (skinProperty == null) {
                    callback.accept(DisguiseCallback.ERROR);
                    return;
                }

                String value = skinProperty.value;
                String signature = skinProperty.signature;

                disguiseRegistry.setDisguised(player, skinProperty);

                try {
                    clearProperties(gameProfile);
                    putTextureProperty(gameProfile, value, signature);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            Bukkit.getOnlinePlayers().forEach(o -> o.hidePlayer(player));
                            Bukkit.getOnlinePlayers().forEach(o -> o.showPlayer(player));

                            setProfileName(gameProfile, name);
                            setTablistName(player, name);

                            broadcastPlayerInfoPacket("REMOVE", entityPlayer);

                            Class<?> destroyClass = getNMSClass("PacketPlayOutEntityDestroy");
                            int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
                            Object packetDestroy = destroyClass.getConstructor(int[].class).newInstance((Object) new int[]{entityId});
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                sendPacket(online, packetDestroy);
                            }

                            String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
                            changeField(entityPlayer, displayField, name);

                            broadcastPlayerInfoPacket("ADD", entityPlayer);

                            Class<?> spawnClass = getNMSClass("PacketPlayOutNamedEntitySpawn");
                            Object packetSpawn = getConstructorWithParameterExact(spawnClass, 1)
                                    .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                sendPacket(online, packetSpawn);
                            }

                            sendEquipmentPackets(entityPlayer, entityId);

                            int held = player.getInventory().getHeldItemSlot();
                            sendPacket(player, packetDestroy);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                moveToWorld(player);
                                player.setFlying(flying);
                                player.setAllowFlight(allowFlight);
                                player.getInventory().setHeldItemSlot(held);
                                player.updateInventory();
                            });

                            String rawPrefix = getRankString(rank, "prefix", "");
                            String nameColor = getRankString(rank, "name-color", "&f");
                            String coloredPrefix = CC.translate(rawPrefix);

                            disguiseRegistry.publishDisguiseState(player, name, rank, skin, nameColor, coloredPrefix);
                            disguiseRegistry.getEffectiveColor(player, color -> {
                                if (plugin.getNameTagColorManager() != null) {
                                    plugin.getNameTagColorManager().applyDisguise(player, name, nameColor);
                                }
                            });

                            Bukkit.getPluginManager().callEvent(new PlayerDisguiseEvent(player, player.getName(), name, rank));
                            callback.accept(DisguiseCallback.SUCCESS);
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.accept(DisguiseCallback.ERROR);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.accept(DisguiseCallback.ERROR);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(DisguiseCallback.ERROR);
        }
    }

    @Override
    public void unDisguise(final Player player, final boolean save, Consumer<DisguiseCallback> callback) {
        final DisguiseData disguiseData = plugin.getDisguiseDataMap().get(player.getUniqueId());
        if (disguiseData == null) {
            if (plugin.getNameTagColorManager() != null) {
                plugin.getNameTagColorManager().clearDisguise(player);
            }
            plugin.getRankManager().refreshPlayerDisplay(player);
            disguiseRegistry.publishClearDisguise(player);
            callback.accept(DisguiseCallback.NOT_DISGUISED);
            return;
        }

        final boolean flying = player.isFlying();
        final boolean allowFlight = player.getAllowFlight();

        try {
            final Object entityPlayer = Utilities.getEntityPlayer(player);
            final Object gameProfile = getGameProfile(entityPlayer);
            final JsonObject profileData = disguiseData.getInfo();

            if (profileData.has("properties")) {
                JsonArray propsJson = profileData.get("properties").getAsJsonArray();
                if (propsJson.size() > 0) {
                    JsonObject first = propsJson.get(0).getAsJsonObject();
                    if (first.has("value") && first.has("signature")) {
                        clearProperties(gameProfile);
                        putTextureProperty(gameProfile, first.get("value").getAsString(), first.get("signature").getAsString());
                    }
                }
            }

            runSyncOrInline(() -> {
                Bukkit.getOnlinePlayers().forEach(o -> o.hidePlayer(player));
                Bukkit.getOnlinePlayers().forEach(o -> o.showPlayer(player));
            });

            broadcastPlayerInfoPacket("REMOVE", entityPlayer);

            Class<?> destroyClass = getNMSClass("PacketPlayOutEntityDestroy");
            int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
            Object packetDestroy = destroyClass.getConstructor(int[].class).newInstance((Object) new int[]{entityId});
            for (Player online : Bukkit.getOnlinePlayers()) {
                sendPacket(online, packetDestroy);
            }

            final String originalName = profileData.get("name").getAsString();
            String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
            setProfileName(gameProfile, originalName);
            changeField(entityPlayer, displayField, originalName);
            setTablistName(player, originalName);

            if (!save) {
                plugin.getDisguiseDataMap().remove(player.getUniqueId());
                disguiseRanks.remove(player.getUniqueId());
                disguiseRegistry.clear(player);
                if (plugin.getNameTagColorManager() != null) {
                    plugin.getNameTagColorManager().clearDisguise(player);
                }
                plugin.getRankManager().refreshPlayerDisplay(player);
                disguiseRegistry.publishClearDisguise(player);
                Bukkit.getPluginManager().callEvent(new PlayerUnDisguiseEvent(player, disguiseData.getName(), originalName, disguiseData.getRank()));
                callback.accept(DisguiseCallback.SUCCESS);
                return;
            }

            broadcastPlayerInfoPacket("ADD", entityPlayer);

            Class<?> spawnClass = getNMSClass("PacketPlayOutNamedEntitySpawn");
            Object packetSpawn = getConstructorWithParameterExact(spawnClass, 1)
                    .newInstance(safeCastTo(entityPlayer, getNMSClass("EntityHuman")));
            for (Player online : Bukkit.getOnlinePlayers()) {
                sendPacket(online, packetSpawn);
            }

            sendEquipmentPackets(entityPlayer, entityId);

            int held = player.getInventory().getHeldItemSlot();
            sendPacket(player, packetDestroy);
            runSyncOrInline(() -> {
                moveToWorld(player);
                player.setFlying(flying);
                player.setAllowFlight(allowFlight);
                player.getInventory().setHeldItemSlot(held);
                player.updateInventory();
            });

            plugin.getDisguiseDataMap().remove(player.getUniqueId());
            disguiseRanks.remove(player.getUniqueId());
            disguiseRegistry.clear(player);
            if (plugin.getNameTagColorManager() != null) {
                plugin.getNameTagColorManager().clearDisguise(player);
            }
            plugin.getRankManager().refreshPlayerDisplay(player);
            disguiseRegistry.publishClearDisguise(player);
            Bukkit.getPluginManager().callEvent(new PlayerUnDisguiseEvent(player, disguiseData.getName(), originalName, disguiseData.getRank()));

            callback.accept(DisguiseCallback.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(DisguiseCallback.ERROR);
        }
    }

    @Override
    public void disguise(Player player, String targetName, Consumer<DisguiseCallback> callback) {
        String version = Utilities.getServerVersion();
        if (!(Utilities.IS_1_7 || version.startsWith("v1_8"))) {
            callback.accept(DisguiseCallback.ERROR);
            return;
        }
        if (player == null || !player.isOnline()) {
            callback.accept(DisguiseCallback.NOT_ONLINE);
            return;
        }
        if (targetName.equalsIgnoreCase(player.getName())) {
            callback.accept(DisguiseCallback.SAME_NAME);
            return;
        }

        if (isDisguiseNameTakenOnNetwork(targetName, player.getUniqueId())) {
            callback.accept(DisguiseCallback.GLOBAL_PLAYER_FOUND);
            return;
        }

        try {
            if (!plugin.getDisguiseDataMap().containsKey(player.getUniqueId())) {
                Object entityPlayer = Utilities.getEntityPlayer(player);
                Object gameProfile = getGameProfile(entityPlayer);
                JsonObject data = new JsonObject();
                data.addProperty("name", player.getName());
                data.addProperty("uuid", player.getUniqueId().toString());
                JsonArray props = new JsonArray();
                serializeProperties(gameProfile, props);
                data.add("properties", props);
                plugin.getDisguiseDataMap().put(player.getUniqueId(),
                        new DisguiseData("", player.getName(), player.getName(), data, System.currentTimeMillis()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(DisguiseCallback.ERROR);
            return;
        }

        disguise(player, "", targetName, targetName, callback);
    }

    @Override
    public void undisguise(Player player, Consumer<DisguiseCallback> callback) {
        String version = Utilities.getServerVersion();
        if (!(Utilities.IS_1_7 || version.startsWith("v1_8"))) {
            callback.accept(DisguiseCallback.ERROR);
            return;
        }
        if (player == null || !player.isOnline()) {
            callback.accept(DisguiseCallback.NOT_ONLINE);
            return;
        }
        unDisguise(player, true, callback);
    }

    private void sendEquipmentPackets(Object entityPlayer, int entityId) {
        String version = Utilities.getServerVersion();
        if (Utilities.IS_1_7 || version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(i -> {
                try {
                    Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                    Object pkt = packetEquip.getConstructor(Integer.TYPE, Integer.TYPE, getNMSClass("ItemStack"))
                            .newInstance(entityId, i,
                                    safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE).invoke(entityPlayer, i), getNMSClass("ItemStack")));
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        sendPacket(online, pkt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            try {
                Class<?> enumSlotsClass = getNMSClass("EnumItemSlot");
                for (Object constant : enumSlotsClass.getEnumConstants()) {
                    Class<?> packetEquip = getNMSClass("PacketPlayOutEntityEquipment");
                    Object pkt = packetEquip.getConstructor(Integer.TYPE, enumSlotsClass, getNMSClass("ItemStack"))
                            .newInstance(entityId, constant,
                                    safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass).invoke(entityPlayer, constant), getNMSClass("ItemStack")));
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        sendPacket(online, pkt);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getRankString(String rank, String key, String def) {
        org.bukkit.configuration.ConfigurationSection section =
                plugin.getRankManager().getRanksSection().getConfigurationSection(rank);
        return section != null ? section.getString(key, def) : def;
    }

    private void runSyncOrInline(Runnable task) {
        if (WinterCore.isShuttingDown) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private boolean isDisguiseNameTakenOnNetwork(String requestedName, UUID selfUuid) {
        if (requestedName == null || requestedName.trim().isEmpty()) {
            return false;
        }

        boolean localConflict = Bukkit.getOnlinePlayers().stream()
                .anyMatch(online -> online.getName().equalsIgnoreCase(requestedName)
                        && !online.getUniqueId().equals(selfUuid));
        if (localConflict) {
            return true;
        }

        return plugin.getNRS().isNameOnlineElsewhere(requestedName, selfUuid);
    }
}
