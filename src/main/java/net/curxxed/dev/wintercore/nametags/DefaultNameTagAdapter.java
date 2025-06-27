package net.curxxed.dev.wintercore.nametags;

import java.util.Map;
import java.util.UUID;
import net.curxxed.dev.wintercore.managers.Handler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class DefaultNameTagAdapter extends Handler implements NameTagAdapter {
    private static final String TEAM_PREFIX = "wintercore_disguise_";

    public DefaultNameTagAdapter(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void setNameTag(Player p, String nameColor) {
        if (p == null || nameColor == null) return;
        String formattedColor = ChatColor.translateAlternateColorCodes('&', nameColor);
        Scoreboard board = p.getScoreboard();
        String teamName = ("wcd_" + p.getName()).substring(0, Math.min(16, ("wcd_" + p.getName()).length()));
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.setPrefix(formattedColor);
        team.setSuffix("");
        team.addEntry(p.getName());
        p.setPlayerListName(formattedColor + p.getName() + ChatColor.RESET);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(p)) continue;
            Scoreboard viewerBoard = viewer.getScoreboard();
            Team viewerTeam = viewerBoard.getTeam(teamName);
            if (viewerTeam == null) {
                viewerTeam = viewerBoard.registerNewTeam(teamName);
            }
            viewerTeam.setPrefix(formattedColor);
            viewerTeam.setSuffix("");
            viewerTeam.addEntry(p.getName());
        }
    }

    @Override
    public void resetNameTag(Player p) {
        if (p == null) return;
        p.setDisplayName(p.getName());
        p.setPlayerListName(p.getName());
        Scoreboard board = p.getScoreboard();
        String teamName = ("wcd_" + p.getName()).substring(0, Math.min(16, ("wcd_" + p.getName()).length()));
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    public void setNameTags(Player p, Map<UUID, String> colorMap) {
        if (p == null || colorMap == null) return;
        if (p.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        Scoreboard board = p.getScoreboard();
        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = ("wcd_" + target.getName()).substring(0, Math.min(16, ("wcd_" + target.getName()).length()));
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            String color = colorMap.getOrDefault(target.getUniqueId(), ChatColor.WHITE.toString());
            team.setPrefix(ChatColor.translateAlternateColorCodes('&', color));
            team.setSuffix("");
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }
        String selfColor = colorMap.getOrDefault(p.getUniqueId(), ChatColor.WHITE.toString());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(p)) continue;
            Scoreboard viewerBoard = viewer.getScoreboard();
            String teamName = ("wcd_" + p.getName()).substring(0, Math.min(16, ("wcd_" + p.getName()).length()));
            Team viewerTeam = viewerBoard.getTeam(teamName);
            if (viewerTeam == null) {
                viewerTeam = viewerBoard.registerNewTeam(teamName);
            }
            viewerTeam.setPrefix(ChatColor.translateAlternateColorCodes('&', selfColor));
            viewerTeam.setSuffix("");
            if (!viewerTeam.hasEntry(p.getName())) {
                viewerTeam.addEntry(p.getName());
            }
        }
        p.setPlayerListName(ChatColor.translateAlternateColorCodes('&', selfColor) + p.getName() + ChatColor.RESET);
    }

}
