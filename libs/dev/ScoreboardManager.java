package net.curxxed.dev;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ScoreboardManager implements Listener {
    private final Main plugin;
    private final ConfigHandler configHandler;
    private final boolean papiEnabled;
    private final Map<UUID, List<String>> lastSidebarLines = new HashMap<>();

    public ScoreboardManager(Main plugin, ConfigHandler configHandler) {
        this.plugin = plugin;
        this.configHandler = configHandler;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void syncTeams(Player player) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard board = player.getScoreboard();
        for (Team mainTeam : mainBoard.getTeams()) {
            Team playerTeam = board.getTeam(mainTeam.getName());
            if (playerTeam == null) {
                playerTeam = board.registerNewTeam(mainTeam.getName());
            }
            playerTeam.setPrefix(mainTeam.getPrefix());
            playerTeam.setSuffix(mainTeam.getSuffix());
            playerTeam.setAllowFriendlyFire(mainTeam.allowFriendlyFire());
            playerTeam.setCanSeeFriendlyInvisibles(mainTeam.canSeeFriendlyInvisibles());
            // Remove all entries first to avoid duplicates/stale data
            for (String entry : new HashSet<>(playerTeam.getPlayers())) {
                playerTeam.removePlayer(Bukkit.getOfflinePlayer(entry));
            }
            // Add all online players who are in this team
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (mainTeam.hasPlayer(online)) {
                    playerTeam.addPlayer(online);
                }
            }
        }
    }

    public void updateSidebar(Player player) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard board = player.getScoreboard();
        if (board == null || board == mainBoard) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }
        // Use a unique objective name per player
        String objName = ("sidebar_" + player.getName()).length() > 16 ? ("sb_" + player.getUniqueId().toString().substring(0, 13)) : ("sidebar_" + player.getName());
        Objective objective = board.getObjective(objName);
        if (objective == null) {
            objective = board.registerNewObjective(objName, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', configHandler.getConfig().getString("title", "&cDefault Title")));
        List<String> lines = configHandler.getConfig().getStringList("lines");
        List<String> resolvedLines = new ArrayList<>();
        for (String line : lines) {
            String displayLine = line;
            if (papiEnabled) {
                try {
                    Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    java.lang.reflect.Method setPlaceholders = papiClass.getMethod("setPlaceholders", Player.class, String.class);
                    displayLine = (String) setPlaceholders.invoke(null, player, displayLine);
                } catch (Exception ignored) {}
            }
            displayLine = ChatColor.translateAlternateColorCodes('&', displayLine);
            resolvedLines.add(displayLine);
        }
        // Only update if lines changed
        List<String> lastLines = lastSidebarLines.get(player.getUniqueId());
        if (lastLines != null && lastLines.equals(resolvedLines)) {
            return; // No change, skip update
        }
        lastSidebarLines.put(player.getUniqueId(), resolvedLines);
        // Remove old scores for this objective only
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        int score = resolvedLines.size();
        for (String displayLine : resolvedLines) {
            Score scoreLine = objective.getScore(displayLine);
            scoreLine.setScore(score);
            score--;
        }
    }

    public void reload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateSidebar(player);
        }
    }

    public void startScoreboardUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateSidebar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Update every 5 seconds (100 ticks)
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateSidebar(event.getPlayer());
        syncTeams(event.getPlayer());
    }
}