package net.curxxed.dev.wintercore.database.redis.event;

import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class RedisPacketReceivedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final RedisPacket<?> packet;

    public RedisPacketReceivedEvent(RedisPacket<?> packet) {
        this.packet = packet;
    }

    public RedisPacket<?> getPacket() {
        return packet;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}