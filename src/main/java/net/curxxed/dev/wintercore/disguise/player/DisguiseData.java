package net.curxxed.dev.wintercore.disguise.player;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisguiseData
{
    private String rank;
    private String name;
    private String skinName;
    private JsonObject info;
    private long lastActivity;

    public DisguiseData(final String rank, final String name, final String skinName, final JsonObject info, final long lastActivity) {
        this.rank = rank;
        this.name = name;
        this.skinName = skinName;
        this.info = info;
        this.lastActivity = lastActivity;
    }
}
