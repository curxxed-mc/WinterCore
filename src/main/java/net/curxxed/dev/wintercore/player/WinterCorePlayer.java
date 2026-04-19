package net.curxxed.dev.wintercore.player;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import java.util.UUID;

@Getter @Setter
public class WinterCorePlayer {
    private final UUID uuid;
    private final String name;

    private String rank = "Default";
    private String tag = "";
    private String chatColorCode = "&f";
    private boolean muted = false;

    public WinterCorePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public ChatColor getMessageColor() {
        try {
            return ChatColor.getByChar(chatColorCode.replace("&", "").charAt(0));
        } catch (Exception e) {
            return ChatColor.WHITE;
        }
    }
}