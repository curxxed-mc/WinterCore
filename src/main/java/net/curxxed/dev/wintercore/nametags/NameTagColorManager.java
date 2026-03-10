package net.curxxed.dev.wintercore.nametags;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagColorManager implements Listener {

    private static final String TEAM_PREFIX    = "wc_";
    private static final String FALLBACK_RANK  = "Default";
    private static final String FALLBACK_COLOR = "&f";

    /**
     * The prefix applied above a staff-mode player's head.
     * §7 makes the asterisk AND the name that follows both render gray.
     * 2 chars — well within the 16-char 1.7/1.8 team-prefix limit.
     */
    private static final String STAFF_PREFIX = CC.translate("&7*");

    private final WinterCore plugin;

    // Per-viewer scoreboards so every player sees the same tags independently.
    private final Map<UUID, Scoreboard> viewerBoards   = new ConcurrentHashMap<>();
    // Last-known rank for each online player (used when staff mode ends).
    private final Map<UUID, String>     rankCache      = new ConcurrentHashMap<>();
    // Explicit color override (set by disguise system, chat-color command, etc.)
    private final Map<UUID, String>     colorOverrides = new ConcurrentHashMap<>();
    // Players currently in staff mode.
    private final Set<UUID>             staffMode      = ConcurrentHashMap.newKeySet();

    // Rank name -> scoreboard team key, rebuilt on load/reload.
    private volatile Map<String, String> rankTeamKey = Collections.emptyMap();

    public NameTagColorManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    // lifecycle

    public void load() {
        rebuildRankIndex();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player p : Utilities.getOnlinePlayers()) {
            viewerBoards.put(p.getUniqueId(), createBoardForViewer(p));
        }
    }

    public void unload() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Utilities.getOnlinePlayers()) {
            try { p.setScoreboard(main); } catch (Exception ignored) {}
        }
        viewerBoards.clear();
        rankCache.clear();
        colorOverrides.clear();
        staffMode.clear();
    }

    // public API

    /**
     * Called by RankDisplayManager / RankManager when a player's rank changes.
     * Stores the rank and triggers a board refresh. If the player is currently
     * in staff mode the stored rank is preserved silently and the staff tag
     * keeps showing - it will snap back to the rank color when staff mode ends.
     */
    public void applyRank(Player player, String rankName) {
        colorOverrides.remove(player.getUniqueId());
        rankCache.put(player.getUniqueId(), rankName);
        scheduleRefresh(player);   // applyToBoard checks staffMode internally
    }

    /**
     * Called when an explicit color override is set (disguise, chat-color, etc.).
     * Same guard as applyRank - staff tag wins if active.
     */
    public void applyColor(Player player, String color) {
        colorOverrides.put(player.getUniqueId(), color != null ? color : FALLBACK_COLOR);
        scheduleRefresh(player);
    }

    /**
     * Toggles the gray-asterisk staff-mode prefix for the player.
     * This is the ONLY place staffMode is mutated, keeping enable/disable
     * symmetrical and preventing stale state.
     */
    public void setStaffMode(Player player, boolean active) {
        if (active) {
            staffMode.add(player.getUniqueId());
        } else {
            staffMode.remove(player.getUniqueId());
        }
        // A single refresh re-renders with or without the staff prefix.
        scheduleRefresh(player);
    }

    public void refresh(Player target) {
        scheduleRefresh(target);
    }

    public void fullRefresh() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            rebuildRankIndex();
            for (Scoreboard board : viewerBoards.values()) {
                for (Player target : Utilities.getOnlinePlayers()) {
                    applyToBoard(board, target);
                }
            }
        });
    }

    // events

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        viewerBoards.put(joining.getUniqueId(), createBoardForViewer(joining));
        for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
            if (entry.getKey().equals(joining.getUniqueId())) continue;
            applyToBoard(entry.getValue(), joining);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        UUID   uuid    = leaving.getUniqueId();
        String name    = leaving.getName();

        viewerBoards.remove(uuid);
        rankCache.remove(uuid);
        colorOverrides.remove(uuid);
        staffMode.remove(uuid);

        for (Scoreboard board : viewerBoards.values()) {
            removeEntryFromBoard(board, name);
        }
    }

    // core rendering

    /**
     * The single source of truth for what prefix a player receives on a board.
     * All public mutators call this through scheduleRefresh.
     *
     *   Staff mode ON  ->  "§7*"  (gray; bleeds into name -> §7*PlayerName)
     *   Staff mode OFF ->  rank/override color code  (e.g. §a -> §aPlayerName)
     */
    private void applyToBoard(Scoreboard board, Player target) {
        UUID   uuid     = target.getUniqueId();
        String rankName = rankCache.getOrDefault(uuid, FALLBACK_RANK);
        String teamKey  = rankTeamKey.getOrDefault(rankName, TEAM_PREFIX + "99");
        String entry    = target.getName();

        final String prefix;
        if (staffMode.contains(uuid)) {
            // Gray asterisk bleeds into the name: §7* + PlayerName = §7*PlayerName
            prefix = STAFF_PREFIX;
        } else {
            String rawColor = colorOverrides.containsKey(uuid)
                    ? colorOverrides.get(uuid)
                    : getRankColor(rankName);
            String translated = CC.translate(rawColor);
            prefix = translated.length() > 16 ? translated.substring(0, 16) : translated;
        }

        Team team = board.getTeam(teamKey);
        if (team == null) team = board.registerNewTeam(teamKey);

        // Move player out of any wrong team first.
        for (Team t : board.getTeams()) {
            if (!t.getName().equals(teamKey) && t.hasEntry(entry)) {
                t.removeEntry(entry);
            }
        }
        if (!team.hasEntry(entry)) team.addEntry(entry);

        team.setPrefix(prefix);
        team.setSuffix("");

        // Team#setColor (1.13+) for accurate tab-list color; skip in staff mode.
        if (!staffMode.contains(uuid) && NameTagVersionHelper.HAS_TEAM_COLOR_API) {
            org.bukkit.ChatColor primary = NameTagVersionHelper.extractPrimaryColor(prefix);
            if (primary != null) {
                try {
                    team.getClass()
                            .getMethod("setColor", org.bukkit.ChatColor.class)
                            .invoke(team, primary);
                } catch (Exception ignored) {}
            }
        }
    }

    // helpers

    private void scheduleRefresh(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Scoreboard board : viewerBoards.values()) {
                applyToBoard(board, target);
            }
        });
    }

    private Scoreboard createBoardForViewer(Player viewer) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        for (Player target : Utilities.getOnlinePlayers()) {
            applyToBoard(board, target);
        }
        viewer.setScoreboard(board);
        return board;
    }

    private void removeEntryFromBoard(Scoreboard board, String entryName) {
        for (Team team : board.getTeams()) {
            if (team.hasEntry(entryName)) {
                team.removeEntry(entryName);
                return;
            }
        }
    }

    private void rebuildRankIndex() {
        ConfigurationSection section = plugin.getRankManager().getRanksSection();
        if (section == null) {
            rankTeamKey = Collections.emptyMap();
            return;
        }
        List<String> names = new ArrayList<>(section.getKeys(false));
        names.sort((a, b) -> {
            int wa = section.getInt(a + ".weight", 0);
            int wb = section.getInt(b + ".weight", 0);
            int cmp = Integer.compare(wb, wa);
            return cmp != 0 ? cmp : a.compareTo(b);
        });
        Map<String, String> built = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            built.put(names.get(i), TEAM_PREFIX + String.format("%02d", i));
        }
        rankTeamKey = Collections.unmodifiableMap(built);
    }

    private String getRankColor(String rankName) {
        ConfigurationSection section = plugin.getRankManager().getRanksSection();
        if (section == null) return FALLBACK_COLOR;
        return section.getString(rankName + ".name-color", FALLBACK_COLOR);
    }
}