package net.curxxed.dev.icore.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
@Setter
public class PlayerDisguiseEvent  extends iCoreEvents{
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
}
