package net.curxxed.dev.wintercore.events.network;

import lombok.Getter;
import net.curxxed.dev.wintercore.annotation.ForRemoval;
import net.curxxed.dev.wintercore.events.WinterCoreEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

@Getter
@Deprecated
@ApiStatus.ScheduledForRemoval
@ForRemoval("Replaced by Packet-based synchronization")
public class RankTagSyncEvent extends WinterCoreEvents {

    private static final HandlerList handlers = new HandlerList();

    public enum SyncType { RANKS, TAGS, ALL }

    private final SyncType syncType;

    /**
     * The sender who triggered the network, or null if this server is
     * receiving the network (i.e. it did not originate here).
     */
    private final @Nullable CommandSender initiator;

    private final String sourceServer;

    public RankTagSyncEvent(SyncType syncType,@Nullable CommandSender initiator, String sourceServer) {
        this.syncType    = syncType;
        this.initiator   = initiator;
        this.sourceServer = sourceServer;
    }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}