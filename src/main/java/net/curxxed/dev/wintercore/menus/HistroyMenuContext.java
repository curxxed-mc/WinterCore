package net.curxxed.dev.wintercore.menus;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class HistroyMenuContext {
    private final String playerName;
    private final UUID uuid;
    private String currentCategory;

    public HistroyMenuContext(String playerName, UUID uuid) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.currentCategory = null;
    }
}
