package net.curxxed.dev.wintercore.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
@Setter
public class PlayerDisguiseEvent  extends WinterCoreEvents {
    private static final HandlerList handlers = new HandlerList();

    private Player player;

    private String previousName;

    private String currentName;

    private String disguisedRank;

    public PlayerDisguiseEvent(Player player, String previousName, String currentName, String disguisedRank) {
        this.player = player;
        this.previousName = previousName;
        this.currentName = currentName;
        this.disguisedRank = disguisedRank;
    }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
