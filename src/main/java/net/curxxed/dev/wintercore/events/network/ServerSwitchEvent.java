package net.curxxed.dev.wintercore.events.network;

import lombok.Getter;
import net.curxxed.dev.wintercore.annotation.ForRemoval;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;

@Getter
@Deprecated
@ApiStatus.ScheduledForRemoval
@ForRemoval("Replaced by Packet-based synchronization")
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