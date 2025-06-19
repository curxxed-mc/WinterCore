package net.curxxed.dev.icore.nametags;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTag implements NameTagAdapter {

    @Override
    public void setNameTag(Player player, String prefix, ChatColor color) {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(player.getName());

            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }
            String formattedColor = color != null ? color.toString() : ChatColor.RESET.toString();
            team.setPrefix(formattedColor); // Only use color, no prefix
            team.setSuffix("");

            team.setNameTagVisibility(NameTagVisibility.ALWAYS);
            team.addEntry(player.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetNameTag(Player player) {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(player.getName());

            if (team != null) {
                team.unregister();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
