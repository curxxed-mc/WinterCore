package net.curxxed.dev.wintercore.rank;

import lombok.Getter;
import net.curxxed.dev.wintercore.managers.events.WinterCoreEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
public class RankChangeEvent extends WinterCoreEvents {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String newRank;

    public RankChangeEvent(Player player, String newRank) {
        this.player = player;
        this.newRank = newRank;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}