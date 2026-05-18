package net.curxxed.dev.wintercore.nms;

import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PacketSender {

    private final Logger logger;

    public PacketSender(Logger logger) {
        this.logger = logger;
    }

    public boolean sendPacket(Player player, Object packet) {
        if (player == null || packet == null) {
            return false;
        }

        try {
            Object connection = resolveConnection(player);
            if (connection == null) {
                return false;
            }

            Method send = resolveSendMethod(connection, packet);
            if (send == null) {
                return false;
            }

            send.setAccessible(true);
            if (send.getParameterTypes().length == 1) {
                send.invoke(connection, packet);
            } else {
                send.invoke(connection, packet, null);
            }
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.FINE, "Could not send packet " + packet.getClass().getName()
                        + " to " + player.getName(), e);
            }
            return false;
        }
    }

    public Object resolveConnection(Player player) {
        try {
            Object handle = Utilities.getEntityPlayer(player);
            Object named = findFieldValue(handle, "playerConnection", "connection", "c");
            if (named != null && isConnectionLike(named)) {
                return named;
            }

            Object discovered = findFirstFieldValue(handle, PacketSender::isConnectionLike);
            return discovered != null ? discovered : named;
        } catch (Exception e) {
            if (logger != null) {
                logger.log(Level.FINE, "Could not resolve connection for " + player.getName(), e);
            }
            return null;
        }
    }

    private Method resolveSendMethod(Object connection, Object packet) {
        Class<?> packetClass = packet.getClass();
        Method[] methods = connection.getClass().getMethods();
        Arrays.sort(methods, Comparator.comparingInt(this::sendMethodPriority));

        for (Method method : methods) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 && parameters.length != 2) {
                continue;
            }
            if (parameters.length == 2 && !allowsNullableSecondParameter(parameters[1])) {
                continue;
            }
            if (parameters[0].isAssignableFrom(packetClass)) {
                return method;
            }
        }

        Class<?> type = connection.getClass();
        while (type != null) {
            methods = type.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparingInt(this::sendMethodPriority));
            for (Method method : methods) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length != 1 && parameters.length != 2) {
                    continue;
                }
                if (parameters.length == 2 && !allowsNullableSecondParameter(parameters[1])) {
                    continue;
                }
                if (parameters[0].isAssignableFrom(packetClass)) {
                    return method;
                }
            }
            type = type.getSuperclass();
        }

        return null;
    }

    private int sendMethodPriority(Method method) {
        String name = method.getName();
        if ("sendPacket".equals(name)) {
            return 0;
        }
        if ("send".equals(name)) {
            return 1;
        }
        if ("a".equals(name)) {
            return 2;
        }
        return 10;
    }

    private boolean allowsNullableSecondParameter(Class<?> parameter) {
        return !parameter.isPrimitive();
    }

    private static boolean isConnectionLike(Object value) {
        if (value == null) {
            return false;
        }
        String name = value.getClass().getName();
        return name.contains("PlayerConnection")
                || name.contains("ServerGamePacketListener")
                || name.contains("ServerCommonPacketListener")
                || name.contains("ServerConfigurationPacketListener");
    }

    private static Object findFieldValue(Object target, String... names) {
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

    private static Object findFirstFieldValue(Object target, FieldValuePredicate predicate) {
        if (target == null) {
            return null;
        }

        Class<?> type = target.getClass();
        while (type != null) {
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(target);
                    if (predicate.test(value)) {
                        return value;
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private interface FieldValuePredicate {
        boolean test(Object value);
    }
}
