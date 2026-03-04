package net.curxxed.dev.wintercore.rank;

import lombok.Getter;
import net.curxxed.dev.wintercore.events.WinterCoreEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
public class RankChangeEvent extends WinterCoreEvents {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String newRank;
    private final String oldRank;

    public RankChangeEvent(Player player, String newRank, String oldRank) {
        this.player = player;
        this.newRank = newRank;
        this.oldRank = oldRank;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}