package net.curxxed.dev.icore.nametags;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class DefaultNameTagAdapter implements NameTagAdapter {
    private static final String TEAM_PREFIX = "icore_disguise_";

    @Override
    public void setNameTag(Player p, String prefix, ChatColor color) {
        if (p == null || color == null) return;
        // Set tab list name
        p.setPlayerListName(color + p.getName() + ChatColor.RESET);
        // Set scoreboard team for name above head
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        // Team name must be <= 16 chars
        String teamName = ("icd_" + p.getName()).substring(0, Math.min(16, ("icd_" + p.getName()).length()));
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        // Only use color for the team prefix
        team.setPrefix(color.toString());
        team.setSuffix("");
        team.addEntry(p.getName());
        // Also set the player's display name for chat (optional)
        p.setDisplayName(color + prefix + p.getName() + ChatColor.RESET);
    }

    @Override
    public void resetNameTag(Player p) {
        if (p == null) return;
        p.setDisplayName(p.getName());
        p.setPlayerListName(p.getName());
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = ("icd_" + p.getName()).substring(0, Math.min(16, ("icd_" + p.getName()).length()));
        Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.removeEntry(p.getName());
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }
}
