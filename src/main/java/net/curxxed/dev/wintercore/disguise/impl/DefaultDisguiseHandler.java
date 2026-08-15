package net.curxxed.dev.wintercore.disguise.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.disguise.packet.DisguisePacketAdapter;
import net.curxxed.dev.wintercore.disguise.packet.DisguisePacketAdapters;
import net.curxxed.dev.wintercore.disguise.player.DisguiseData;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DefaultDisguiseHandler extends DisguiseHandler {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<UUID, String> disguiseRanks = new ConcurrentHashMap<>();
    private final DisguiseRegistry disguiseRegistry;
    private final DisguisePacketAdapter packetAdapter;

    private final Class<?> gameProfileClass;
    private final Class<?> propertyClass;
    private final Class<?> paperProfileClass;
    private final Class<?> paperPropertyClass;

    public DefaultDisguiseHandler(final WinterCore plugin, final DisguiseRegistry disguiseRegistry) {
        super(plugin);
        this.disguiseRegistry = disguiseRegistry;
        this.packetAdapter = DisguisePacketAdapters.create(plugin, this);
        this.gameProfileClass = Utilities.resolveAuthlibClass("GameProfile");
        this.propertyClass = Utilities.resolveAuthlibClass("properties.Property");
        this.paperProfileClass = optionalClass("com.destroystokyo.paper.profile.PlayerProfile");
        this.paperPropertyClass = optionalClass("com.destroystokyo.paper.profile.ProfileProperty");
    }

    public String getPacketAdapterName() {
        return packetAdapter.name();
    }

    private Class<?> optionalClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private Object getProfile(Player player, Object entityPlayer) throws Exception {
        if (paperProfileClass != null) {
            return player.getClass().getMethod("getPlayerProfile").invoke(player);
        }
        return getGameProfile(entityPlayer);
    }

    private Object getGameProfile(Object entityPlayer) throws Exception {
        try {
            return entityPlayer.getClass().getMethod("getProfile").invoke(entityPlayer);
        } catch (NoSuchMethodException ignored) {
            try {
                return entityPlayer.getClass().getMethod("getGameProfile").invoke(entityPlayer);
            } catch (NoSuchMethodException ignoredAgain) {
                return findGameProfileField(entityPlayer);
            }
        }
    }

    private Object getProperties(Object gameProfile) throws Exception {
        if (paperProfileClass != null && paperProfileClass.isInstance(gameProfile)) {
            return paperProfileClass.getMethod("getProperties").invoke(gameProfile);
        }
        return invokeAccessor(gameProfile, gameProfileClass, "getProperties", "properties");
    }

    private void clearProperties(Object gameProfile) throws Exception {
        if (paperProfileClass != null && paperProfileClass.isInstance(gameProfile)) {
            paperProfileClass.getMethod("clearProperties").invoke(gameProfile);
            return;
        }
        getProperties(gameProfile).getClass().getMethod("clear").invoke(getProperties(gameProfile));
    }

    private void putTextureProperty(Object gameProfile, String value, String signature) throws Exception {
        if (paperProfileClass != null && paperProfileClass.isInstance(gameProfile)) {
            Constructor<?> constructor = paperPropertyClass.getConstructor(String.class, String.class, String.class);
            paperProfileClass.getMethod("setProperty", paperPropertyClass)
                    .invoke(gameProfile, constructor.newInstance("textures", value, signature));
            return;
        }
        Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class, String.class);
        Object property = propCtor.newInstance("textures", value, signature);
        Object propertiesMap = getProperties(gameProfile);
        propertiesMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertiesMap, "textures", property);
    }

    private void serializeProperties(Object gameProfile, JsonArray out) throws Exception {
        Object propertiesMap = getProperties(gameProfile);
        if (paperProfileClass != null && paperProfileClass.isInstance(gameProfile)) {
            for (Object property : (Collection<?>) propertiesMap) {
                JsonObject obj = new JsonObject();
                String name = (String) paperPropertyClass.getMethod("getName").invoke(property);
                obj.addProperty("key", name);
                obj.addProperty("value-name", name);
                obj.addProperty("value", (String) paperPropertyClass.getMethod("getValue").invoke(property));
                obj.addProperty("signature", (String) paperPropertyClass.getMethod("getSignature").invoke(property));
                out.add(obj);
            }
            return;
        }
        Collection<Map.Entry<Object, Object>> entries =
                (Collection<Map.Entry<Object, Object>>) propertiesMap.getClass().getMethod("entries").invoke(propertiesMap);

        for (Map.Entry<Object, Object> entry : entries) {
            Object prop = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("key", (String) entry.getKey());
            obj.addProperty("value-name", (String) invokeAccessor(prop, propertyClass, "getName", "name"));
            obj.addProperty("value", (String) invokeAccessor(prop, propertyClass, "getValue", "value"));
            obj.addProperty("signature", (String) invokeAccessor(prop, propertyClass, "getSignature", "signature"));
            out.add(obj);
        }
    }

    private Object invokeAccessor(Object target, Class<?> type, String oldName, String newName) throws Exception {
        try {
            return type.getMethod(oldName).invoke(target);
        } catch (NoSuchMethodException ignored) {
            return type.getMethod(newName).invoke(target);
        }
    }

    private void setProfileName(Player player, Object gameProfile, String newName) {
        if (paperProfileClass != null && paperProfileClass.isInstance(gameProfile)) {
            try {
                paperProfileClass.getMethod("setName", String.class).invoke(gameProfile, newName);
                player.getClass().getMethod("setPlayerProfile", paperProfileClass).invoke(player, gameProfile);
                return;
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            Object profile = player.getClass().getMethod("getPlayerProfile").invoke(player);
            profile.getClass().getMethod("setName", String.class).invoke(profile, newName);
            for (java.lang.reflect.Method method : player.getClass().getMethods()) {
                if (method.getName().equals("setPlayerProfile") && method.getParameterCount() == 1) {
                    method.invoke(player, profile);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        changeField(gameProfile, "name", newName);
    }

    private Object findGameProfileField(Object entityPlayer) throws Exception {
        Class<?> type = entityPlayer.getClass();
        while (type != null) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (gameProfileClass.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field.get(entityPlayer);
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchFieldException("GameProfile");
    }

    private void setTablistName(Player player, String name) {
        try {
            player.setDisplayName(name);
        } catch (Throwable ignored) {
        }
        try {
            player.setPlayerListName(name);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void disguise(final Player player, final String rank, final String name, final String skin, Consumer<DisguiseCallback> callback) {
        if (player == null || !player.isOnline()) {
            callback.accept(DisguiseCallback.ERROR);
            return;
        }

        final String effectiveRank = resolveDisguiseRank(player, rank);
        if (plugin.getRankManager().getRanksSection().getConfigurationSection(effectiveRank) == null) {
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
            final Object gameProfile = getProfile(player, entityPlayer);

            final JsonObject data = new JsonObject();
            data.addProperty("name", player.getName());
            data.addProperty("uuid", player.getUniqueId().toString());
            final JsonArray properties = new JsonArray();
            serializeProperties(gameProfile, properties);
            data.add("properties", properties);

            final DisguiseData disguiseData = new DisguiseData(effectiveRank, name, skin, data, System.currentTimeMillis());
            plugin.getDisguiseDataMap().put(player.getUniqueId(), disguiseData);
            disguiseRanks.put(player.getUniqueId(), effectiveRank);

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

                    plugin.getTasks().sync(() -> {
                        try {
                            setProfileName(player, gameProfile, name);
                            setTablistName(player, name);

                            String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
                            changeField(entityPlayer, displayField, name);

                            int held = player.getInventory().getHeldItemSlot();
                            packetAdapter.refresh(player, entityPlayer, () -> {
                                moveToWorld(player);
                                player.setFlying(flying);
                                player.setAllowFlight(allowFlight);
                                player.getInventory().setHeldItemSlot(held);
                                player.updateInventory();
                            });

                            String rawPrefix = getRankString(effectiveRank, "prefix", "");
                            String nameColor = getRankString(effectiveRank, "name-color", "&f");
                            String coloredPrefix = CC.translate(rawPrefix);

                            disguiseRegistry.publishDisguiseState(player, name, effectiveRank, skin, nameColor, coloredPrefix);
                            disguiseRegistry.getEffectiveColor(player, color -> {
                                if (plugin.getNameTagColorManager() != null) {
                                    plugin.getNameTagColorManager().applyDisguise(player, name, nameColor);
                                }
                            });

                            disguiseRegistry.updateColorCache(player);
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
            final Object gameProfile = getProfile(player, entityPlayer);
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

            final String originalName = profileData.get("name").getAsString();
            String displayField = Utilities.IS_1_7 ? "listName" : "displayName";
            setProfileName(player, gameProfile, originalName);
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
                disguiseRegistry.updateColorCache(player);
                callback.accept(DisguiseCallback.SUCCESS);
                return;
            }

            int held = player.getInventory().getHeldItemSlot();
            packetAdapter.refresh(player, entityPlayer, () -> {
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
            disguiseRegistry.updateColorCache(player);

            callback.accept(DisguiseCallback.SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(DisguiseCallback.ERROR);
        }
    }

    @Override
    public void disguise(Player player, String targetName, Consumer<DisguiseCallback> callback) {
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
                Object gameProfile = getProfile(player, entityPlayer);
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

        disguise(player, resolveDisguiseRank(player, null), targetName, targetName, callback);
    }

    @Override
    public void undisguise(Player player, Consumer<DisguiseCallback> callback) {
        if (player == null || !player.isOnline()) {
            callback.accept(DisguiseCallback.NOT_ONLINE);
            return;
        }
        unDisguise(player, true, callback);
    }

    private String getRankString(String rank, String key, String def) {
        org.bukkit.configuration.ConfigurationSection section =
                plugin.getRankManager().getRanksSection().getConfigurationSection(rank);
        return section != null ? section.getString(key, def) : def;
    }

    private String resolveDisguiseRank(Player player, String rank) {
        if (rank != null && !rank.trim().isEmpty()) {
            return rank;
        }

        String current = plugin.getRankManager().getRankSync(player);
        if (current != null && !current.trim().isEmpty()) {
            return current;
        }

        return plugin.getConfig().getString("default-rank", "Default");
    }

    private boolean isDisguiseNameTakenOnNetwork(String requestedName, UUID selfUuid) {
        if (requestedName == null || requestedName.trim().isEmpty()) {
            return false;
        }

        boolean localConflict = net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                .anyMatch(online -> online.getName().equalsIgnoreCase(requestedName)
                        && !online.getUniqueId().equals(selfUuid));
        if (localConflict) {
            return true;
        }

        return plugin.getNRS().isNameOnlineElsewhere(requestedName, selfUuid);
    }
}
