package net.curxxed.dev.wintercore.version;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PlayerProtocolResolver {

    private static final Map<Integer, String> PROTOCOL_NAMES = buildProtocolNames();

    private final WinterCore plugin;

    public PlayerProtocolResolver(WinterCore plugin) {
        this.plugin = plugin;
    }

    public String resolveName(Player player) {
        Integer protocol = resolveProtocol(player);
        if (protocol == null) {
            return plugin.getMessageConfig().get("client-brand.version-unknown", "Unknown (protocol unavailable)");
        }
        return PROTOCOL_NAMES.getOrDefault(protocol, "Protocol " + protocol);
    }

    public Integer resolveProtocol(Player player) {
        if (player == null) {
            return null;
        }

        try {
            Object entityPlayer = Utilities.getEntityPlayer(player);
            Object connection = findFieldValue(entityPlayer, "playerConnection", "connection", "c");
            if (connection == null) {
                return null;
            }

            Object networkManager = findNetworkManager(connection);
            if (networkManager == null) {
                return null;
            }

            return findLikelyProtocol(networkManager);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object findNetworkManager(Object connection) {
        Object byName = findFieldValue(connection, "networkManager", "network", "manager", "a", "b");
        if (byName != null && classNameContains(byName, "NetworkManager")) {
            return byName;
        }

        Class<?> type = connection.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(connection);
                    if (value != null && classNameContains(value, "NetworkManager")) {
                        return value;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }

        return byName;
    }

    private Integer findLikelyProtocol(Object networkManager) {
        Integer named = findIntFieldValue(networkManager, "protocolVersion", "version", "protocol", "handshakeProtocol");
        if (isProtocol(named)) {
            return named;
        }

        Class<?> type = networkManager.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (fieldType != int.class && fieldType != Integer.class) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    Object value = field.get(networkManager);
                    if (value instanceof Integer && isProtocol((Integer) value)) {
                        return (Integer) value;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }

    private Object findFieldValue(Object target, String... names) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private Integer findIntFieldValue(Object target, String... names) {
        Object value = findFieldValue(target, names);
        return value instanceof Integer ? (Integer) value : null;
    }

    private boolean isProtocol(Integer value) {
        return value != null && value > 0 && value < 2000;
    }

    private boolean classNameContains(Object value, String needle) {
        return value.getClass().getName().contains(needle);
    }

    private static Map<Integer, String> buildProtocolNames() {
        Map<Integer, String> names = new HashMap<>();
        names.put(4, "1.7.2-1.7.5");
        names.put(5, "1.7.6-1.7.10");
        names.put(47, "1.8.x");
        names.put(107, "1.9");
        names.put(108, "1.9.1");
        names.put(109, "1.9.2");
        names.put(110, "1.9.4");
        names.put(210, "1.10.x");
        names.put(315, "1.11");
        names.put(316, "1.11.1-1.11.2");
        names.put(335, "1.12");
        names.put(338, "1.12.1");
        names.put(340, "1.12.2");
        names.put(393, "1.13");
        names.put(401, "1.13.1");
        names.put(404, "1.13.2");
        names.put(477, "1.14");
        names.put(480, "1.14.1");
        names.put(485, "1.14.2");
        names.put(490, "1.14.3");
        names.put(498, "1.14.4");
        names.put(573, "1.15");
        names.put(575, "1.15.1");
        names.put(578, "1.15.2");
        names.put(735, "1.16");
        names.put(736, "1.16.1");
        names.put(751, "1.16.2");
        names.put(753, "1.16.3");
        names.put(754, "1.16.4-1.16.5");
        names.put(755, "1.17");
        names.put(756, "1.17.1");
        names.put(757, "1.18-1.18.1");
        names.put(758, "1.18.2");
        names.put(759, "1.19");
        names.put(760, "1.19.1-1.19.2");
        names.put(761, "1.19.3");
        names.put(762, "1.19.4");
        names.put(763, "1.20-1.20.1");
        names.put(764, "1.20.2");
        names.put(765, "1.20.3-1.20.4");
        names.put(766, "1.20.5-1.20.6");
        names.put(767, "1.21-1.21.1");
        names.put(768, "1.21.2-1.21.3");
        names.put(769, "1.21.4");
        names.put(770, "1.21.5");
        names.put(771, "1.21.6");
        names.put(772, "1.21.7-1.21.8");
        names.put(773, "1.21.9-1.21.10");
        names.put(774, "1.21.11");
        names.put(775, "26.1-26.1.1");
        names.put(776, "26.1.2");
        return Collections.unmodifiableMap(names);
    }
}
