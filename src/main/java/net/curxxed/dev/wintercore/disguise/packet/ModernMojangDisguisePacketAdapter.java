package net.curxxed.dev.wintercore.disguise.packet;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

final class ModernMojangDisguisePacketAdapter implements DisguisePacketAdapter {

    private final WinterCore plugin;

    ModernMojangDisguisePacketAdapter(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "mojang-modern";
    }

    @Override
    public void refresh(Player player, Object entityPlayer, Runnable legacySelfRefresh) throws Exception {
        hideShow(player);
        sendPlayerInfoRemove(player, entityPlayer);
        sendDestroy(player, entityPlayer);
        sendPlayerInfoAdd(player, entityPlayer);
        sendSpawn(player, entityPlayer);
    }

    private void hideShow(Player player) {
        for (Player online : Utilities.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.hidePlayer(player);
                online.showPlayer(player);
            }
        }
    }

    private void sendPlayerInfoRemove(Player player, Object entityPlayer) {
        Object packet = createPlayerInfoRemove(player.getUniqueId(), entityPlayer);
        if (packet != null) {
            broadcast(packet, player);
        }
    }

    private void sendPlayerInfoAdd(Player player, Object entityPlayer) {
        Object packet = createPlayerInfoAdd(entityPlayer);
        if (packet != null) {
            broadcast(packet, player);
        }
    }

    private void sendDestroy(Player player, Object entityPlayer) {
        Object packet = createDestroyPacket(entityPlayer);
        if (packet != null) {
            broadcast(packet, player);
        }
    }

    private void sendSpawn(Player player, Object entityPlayer) {
        Object packet = createSpawnPacket(entityPlayer);
        if (packet != null) {
            broadcast(packet, player);
        }
    }

    private Object createPlayerInfoRemove(UUID uuid, Object entityPlayer) {
        try {
            Class<?> removeClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket"
            );
            if (removeClass != null) {
                Constructor<?> constructor = findConstructor(removeClass, Collection.class);
                if (constructor != null) {
                    return constructor.newInstance(Collections.singleton(uuid));
                }
            }
        } catch (Exception ignored) {
        }

        return createLegacyModernPlayerInfoPacket("REMOVE_PLAYER", entityPlayer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object createPlayerInfoAdd(Object entityPlayer) {
        try {
            Class<?> updateClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket"
            );
            Class<?> actionClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action"
            );
            if (updateClass != null && actionClass != null && actionClass.isEnum()) {
                EnumSet actions = EnumSet.noneOf((Class<Enum>) actionClass);
                addEnumIfPresent(actions, actionClass, "ADD_PLAYER");
                addEnumIfPresent(actions, actionClass, "UPDATE_LISTED");
                addEnumIfPresent(actions, actionClass, "UPDATE_DISPLAY_NAME");
                if (actions.isEmpty()) {
                    addEnumIfPresent(actions, actionClass, "ADD_PLAYER");
                }

                Constructor<?> constructor = findConstructor(updateClass, EnumSet.class, Collection.class);
                if (constructor != null) {
                    return constructor.newInstance(actions, Collections.singleton(entityPlayer));
                }
            }
        } catch (Exception ignored) {
        }

        return createLegacyModernPlayerInfoPacket("ADD_PLAYER", entityPlayer);
    }

    private Object createLegacyModernPlayerInfoPacket(String action, Object entityPlayer) {
        try {
            Class<?> packetClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket"
            );
            Class<?> actionClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket$Action"
            );
            if (packetClass == null || actionClass == null || !actionClass.isEnum()) {
                return null;
            }

            Object enumValue = enumValue(actionClass, action);
            Constructor<?> constructor = findConstructor(packetClass, actionClass, Iterable.class);
            return constructor != null
                    ? constructor.newInstance(enumValue, Collections.singleton(entityPlayer))
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object createDestroyPacket(Object entityPlayer) {
        try {
            Class<?> destroyClass = resolveClass(
                    "net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket"
            );
            if (destroyClass == null) {
                return null;
            }

            int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
            Constructor<?> constructor = findConstructor(destroyClass, int[].class);
            return constructor != null
                    ? constructor.newInstance((Object) new int[]{entityId})
                    : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object createSpawnPacket(Object entityPlayer) {
        Object packet = constructWithEntity(
                "net.minecraft.network.protocol.game.ClientboundAddPlayerPacket",
                entityPlayer
        );
        if (packet != null) {
            return packet;
        }

        return constructWithEntity(
                "net.minecraft.network.protocol.game.ClientboundAddEntityPacket",
                entityPlayer
        );
    }

    private Object constructWithEntity(String className, Object entityPlayer) {
        try {
            Class<?> packetClass = resolveClass(className);
            if (packetClass == null) {
                return null;
            }

            for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 1 && parameters[0].isAssignableFrom(entityPlayer.getClass())) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(entityPlayer);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void broadcast(Object packet, Player disguised) {
        for (Player online : Utilities.getOnlinePlayers()) {
            if (!online.equals(disguised)) {
                plugin.getPacketSender().sendPacket(online, packet);
            }
        }
    }

    private Class<?> resolveClass(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private Constructor<?> findConstructor(Class<?> type, Class<?>... parameterTypes) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            Class<?>[] actual = constructor.getParameterTypes();
            if (actual.length != parameterTypes.length) {
                continue;
            }

            boolean matches = true;
            for (int index = 0; index < actual.length; index++) {
                if (!actual[index].isAssignableFrom(parameterTypes[index])
                        && !parameterTypes[index].isAssignableFrom(actual[index])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEnumIfPresent(EnumSet target, Class<?> enumClass, String name) {
        Object value = enumValue(enumClass, name);
        if (value instanceof Enum) {
            target.add((Enum) value);
        }
    }

    private Object enumValue(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
