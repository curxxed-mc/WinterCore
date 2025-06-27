package net.curxxed.dev.wintercore.nametags;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class NameTagBoard {

    private final WinterCore plugin;
    private final UUID uuid;

    private Scoreboard scoreboard;

    public NameTagBoard setup(Player player) {
        if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
            this.scoreboard = player.getScoreboard();
        } else {
            this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        player.setScoreboard(this.scoreboard);

        return this;
    }
}

