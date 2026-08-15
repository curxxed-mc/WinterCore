package net.curxxed.dev.wintercore.disguise.packet;

import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.stream.Stream;

final class LegacyDisguisePacketAdapter implements DisguisePacketAdapter {

    private final DisguiseHandler handler;

    LegacyDisguisePacketAdapter(DisguiseHandler handler) {
        this.handler = handler;
    }

    @Override
    public String name() {
        return "craftbukkit-legacy";
    }

    @Override
    public void refresh(Player player, Object entityPlayer, Runnable legacySelfRefresh) throws Exception {
        hideShow(player);

        broadcastPlayerInfoPacket("REMOVE", entityPlayer);

        int entityId = (int) entityPlayer.getClass().getMethod("getId").invoke(entityPlayer);
        Object destroyPacket = createDestroyPacket(entityId);
        for (Player online : Utilities.getOnlinePlayers()) {
            handler.sendPacket(online, destroyPacket);
        }

        broadcastPlayerInfoPacket("ADD", entityPlayer);
        spawnForViewers(entityPlayer);
        sendEquipmentPackets(entityPlayer, entityId);

        handler.sendPacket(player, destroyPacket);
        legacySelfRefresh.run();
    }

    private void hideShow(Player player) {
        Utilities.getOnlinePlayers().forEach(online -> online.hidePlayer(player));
        Utilities.getOnlinePlayers().forEach(online -> online.showPlayer(player));
    }

    private Object createDestroyPacket(int entityId) throws Exception {
        Class<?> destroyClass = handler.getNMSClass("PacketPlayOutEntityDestroy");
        return destroyClass.getConstructor(int[].class).newInstance((Object) new int[]{entityId});
    }

    private void sendPlayerInfoPacket(String action, Object entityPlayer, Player observer) throws Exception {
        Class<?> packetClass = handler.getNMSClass("PacketPlayOutPlayerInfo");
        Object packet;

        if (Utilities.IS_1_7) {
            String methodName = action.equals("ADD") ? "addPlayer" : "removePlayer";
            packet = packetClass.getMethod(methodName, handler.getNMSClass("EntityPlayer")).invoke(null, entityPlayer);
        } else {
            Class<?> enumClass = handler.doesClassExists("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    ? handler.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction")
                    : handler.getNMSClass("EnumPlayerInfoAction");
            Object enumValue = enumClass.getEnumConstants()[action.equals("ADD") ? 0 : 4];
            packet = packetClass.getConstructor(enumClass, Iterable.class).newInstance(enumValue, Collections.singleton(entityPlayer));
        }

        handler.sendPacket(observer, packet);
    }

    private void broadcastPlayerInfoPacket(String action, Object entityPlayer) throws Exception {
        for (Player online : Utilities.getOnlinePlayers()) {
            sendPlayerInfoPacket(action, entityPlayer, online);
        }
    }

    private void spawnForViewers(Object entityPlayer) throws Exception {
        Class<?> spawnClass = handler.getNMSClass("PacketPlayOutNamedEntitySpawn");
        Object packetSpawn = handler.getConstructorWithParameterExact(spawnClass, 1)
                .newInstance(handler.safeCastTo(entityPlayer, handler.getNMSClass("EntityHuman")));
        for (Player online : Utilities.getOnlinePlayers()) {
            handler.sendPacket(online, packetSpawn);
        }
    }

    private void sendEquipmentPackets(Object entityPlayer, int entityId) {
        String version = Utilities.getServerVersion();
        if (Utilities.IS_1_7 || version.contains("1_8")) {
            Stream.of(0, 1, 2, 3).forEach(slot -> {
                try {
                    Class<?> packetEquip = handler.getNMSClass("PacketPlayOutEntityEquipment");
                    Object packet = packetEquip.getConstructor(Integer.TYPE, Integer.TYPE, handler.getNMSClass("ItemStack"))
                            .newInstance(entityId, slot,
                                    handler.safeCastTo(entityPlayer.getClass().getMethod("getEquipment", Integer.TYPE)
                                            .invoke(entityPlayer, slot), handler.getNMSClass("ItemStack")));
                    for (Player online : Utilities.getOnlinePlayers()) {
                        handler.sendPacket(online, packet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        try {
            Class<?> enumSlotsClass = handler.getNMSClass("EnumItemSlot");
            for (Object constant : enumSlotsClass.getEnumConstants()) {
                Class<?> packetEquip = handler.getNMSClass("PacketPlayOutEntityEquipment");
                Object packet = packetEquip.getConstructor(Integer.TYPE, enumSlotsClass, handler.getNMSClass("ItemStack"))
                        .newInstance(entityId, constant,
                                handler.safeCastTo(entityPlayer.getClass().getMethod("getEquipment", enumSlotsClass)
                                        .invoke(entityPlayer, constant), handler.getNMSClass("ItemStack")));
                for (Player online : Utilities.getOnlinePlayers()) {
                    handler.sendPacket(online, packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
