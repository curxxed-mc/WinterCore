package net.curxxed.dev.wintercore.events.disguise;

import lombok.Getter;
import net.curxxed.dev.wintercore.events.WinterCoreEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@Getter
public class PlayerUnDisguiseEvent extends WinterCoreEvents {
    private static final HandlerList handlers = new HandlerList();

    private Player player;

    private String previousName;

    private String currentName;

    private String disguisedRank;

    public PlayerUnDisguiseEvent(Player player, String previousName, String currentName, String disguisedRank) {
        this.player = player;
        this.previousName = previousName;
        this.currentName = currentName;
        this.disguisedRank = disguisedRank;
    }

    @Override
    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
