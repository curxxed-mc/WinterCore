package net.curxxed.dev.icore.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Setter
@Getter
public class PlayerUnDisguiseEvent extends iCoreEvents {
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

}
