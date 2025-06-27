package net.curxxed.dev.wintercore.nametags;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public interface NameTagAdapter {
    void setNameTag(Player p, String nameColor);
    void resetNameTag(Player p);
    void setNameTags(Player p, Map<UUID, String> colorMap);
}
