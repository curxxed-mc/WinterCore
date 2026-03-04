package net.curxxed.dev.wintercore.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

@Getter
public class ServerSwitchEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String fromServer;
    private final String toServer;

    public ServerSwitchEvent(Player player, String fromServer, String toServer) {
        super(player);
        this.fromServer = fromServer;
        this.toServer = toServer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}