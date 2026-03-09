package net.curxxed.dev.wintercore.events;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

@Getter
public class RankTagSyncEvent extends WinterCoreEvents {

    private static final HandlerList handlers = new HandlerList();

    public enum SyncType { RANKS, TAGS, ALL }

    private final SyncType syncType;

    /**
     * The sender who triggered the sync, or null if this server is
     * receiving the sync (i.e. it did not originate here).
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