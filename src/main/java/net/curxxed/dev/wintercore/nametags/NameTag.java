package net.curxxed.dev.wintercore.nametags;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;

public class NameTag implements NameTagAdapter {

    @Override
    public void setNameTag(Player player, String nameColor) {
        try {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = scoreboard.getTeam(player.getName());

            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }
            String formattedColor = nameColor != null ? ChatColor.translateAlternateColorCodes('&', nameColor) : ChatColor.RESET.toString();
            team.setPrefix(formattedColor);
            team.setSuffix("");

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

    @Override
    public void setNameTags(Player p, Map<UUID, String> colorMap) {

    }
}