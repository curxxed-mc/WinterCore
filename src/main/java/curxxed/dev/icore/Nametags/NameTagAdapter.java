package curxxed.dev.icore.Nametags;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public interface NameTagAdapter {

    void setNameTag(Player p, String prefix, ChatColor color);
    void resetNameTag(Player p);
}
