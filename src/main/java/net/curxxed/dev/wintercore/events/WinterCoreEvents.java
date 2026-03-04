package net.curxxed.dev.wintercore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class WinterCoreEvents extends Event {
    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
