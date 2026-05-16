package net.curxxed.dev.wintercore.player;

import lombok.Getter;
import lombok.Setter;
import net.curxxed.dev.wintercore.utils.CC;

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

    public String getMessageColor() {
        try {
            String raw = chatColorCode == null ? "" : chatColorCode.trim();
            if (raw.isEmpty()) {
                return CC.translate("&f");
            }
            String normalized = raw.startsWith("&") || raw.startsWith(String.valueOf('\u00A7')) ? raw : "&" + raw;
            return normalized.startsWith(String.valueOf('\u00A7')) ? normalized : CC.translate(normalized);
        } catch (Exception e) {
            return CC.translate("&f");
        }
    }
}
