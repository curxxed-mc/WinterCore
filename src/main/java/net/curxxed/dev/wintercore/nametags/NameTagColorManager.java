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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagColorManager implements Listener {

    private static final String TEAM_PREFIX = "wc_";
    private static final String FALLBACK_RANK = "Default";
    private static final String FALLBACK_COLOR = "&f";
    private static final String STAFF_PREFIX = CC.translate("&7*");

    private final WinterCore plugin;
    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();
    private final Map<UUID, String> rankCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorOverrides = new ConcurrentHashMap<>();
    private final Map<UUID, String> disguiseNames = new ConcurrentHashMap<>();
    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();

    private volatile Map<String, String> rankTeamKey = Collections.emptyMap();

    public NameTagColorManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rebuildRankIndex();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player p : Utilities.getOnlinePlayers()) {
            viewerBoards.put(p.getUniqueId(), createBoardForViewer(p));
            updatePlayerListName(p);
        }
    }

    public void unload() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Utilities.getOnlinePlayers()) {
            try {
                p.setScoreboard(main);
                safeSetPlayerListName(p, p.getName());
            } catch (Exception ignored) {
            }
        }

        viewerBoards.clear();
        rankCache.clear();
        colorOverrides.clear();
        disguiseNames.clear();
        staffMode.clear();
    }

    public void applyRank(Player player, String rankName) {
        UUID uuid = player.getUniqueId();
        rankCache.put(uuid, rankName);

        if (!disguiseNames.containsKey(uuid)) {
            colorOverrides.remove(uuid);
        }

        scheduleRefresh(player);
    }

    public void applyColor(Player player, String color) {
        colorOverrides.put(player.getUniqueId(), color != null ? color : FALLBACK_COLOR);
        scheduleRefresh(player);
    }

    public void setStaffMode(Player player, boolean active) {
        if (active) {
            staffMode.add(player.getUniqueId());
        } else {
            staffMode.remove(player.getUniqueId());
        }
        scheduleRefresh(player);
    }

    public void refresh(Player target) {
        scheduleRefresh(target);
    }

    public void fullRefresh() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            rebuildRankIndex();
            for (Player target : Utilities.getOnlinePlayers()) {
                updatePlayerListName(target);
                for (Scoreboard board : viewerBoards.values()) {
                    applyToBoard(board, target);
                }
            }
        });
    }

    public String getVisibleName(Player player) {
        return disguiseNames.getOrDefault(player.getUniqueId(), player.getName());
    }

    public void applyDisguise(Player player, String disguiseName, String color) {
        UUID uuid = player.getUniqueId();
        if (disguiseName == null || disguiseName.isEmpty()) {
            disguiseName = player.getName();
        }

        disguiseNames.put(uuid, disguiseName);
        colorOverrides.put(uuid, color != null ? color : FALLBACK_COLOR);

        updatePlayerListName(player);
        scheduleRefresh(player);
    }

    public void clearDisguise(Player player) {
        UUID uuid = player.getUniqueId();
        disguiseNames.remove(uuid);
        colorOverrides.remove(uuid);

        updatePlayerListName(player);
        scheduleRefresh(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        viewerBoards.put(joining.getUniqueId(), createBoardForViewer(joining));
        updatePlayerListName(joining);

        for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
            if (entry.getKey().equals(joining.getUniqueId())) continue;
            applyToBoard(entry.getValue(), joining);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        UUID uuid = leaving.getUniqueId();
        String name = leaving.getName();

        viewerBoards.remove(uuid);
        rankCache.remove(uuid);
        colorOverrides.remove(uuid);
        staffMode.remove(uuid);
        disguiseNames.remove(uuid);

        for (Scoreboard board : viewerBoards.values()) {
            removeEntryFromBoard(board, name);
        }
    }

    private void applyToBoard(Scoreboard board, Player target) {
        UUID uuid = target.getUniqueId();
        String rankName = rankCache.getOrDefault(uuid, FALLBACK_RANK);
        String teamKey = rankTeamKey.getOrDefault(rankName, TEAM_PREFIX + "99");
        String entry = getVisibleName(target);

        final String prefix;
        if (staffMode.contains(uuid)) {
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

        for (Team t : board.getTeams()) {
            if (!t.getName().equals(teamKey) && t.hasEntry(entry)) {
                t.removeEntry(entry);
            }
        }

        if (!team.hasEntry(entry)) team.addEntry(entry);

        team.setPrefix(prefix);
        team.setSuffix("");

        if (!staffMode.contains(uuid) && NameTagVersionHelper.HAS_TEAM_COLOR_API) {
            org.bukkit.ChatColor primary = NameTagVersionHelper.extractPrimaryColor(prefix);
            if (primary != null) {
                try {
                    team.getClass()
                            .getMethod("setColor", org.bukkit.ChatColor.class)
                            .invoke(team, primary);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void scheduleRefresh(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            updatePlayerListName(target);
            for (Scoreboard board : viewerBoards.values()) {
                applyToBoard(board, target);
            }
        });
    }

    private void updatePlayerListName(Player player) {
        if (player == null || !player.isOnline()) return;
        safeSetPlayerListName(player, getVisibleName(player));
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
            return cmp != 0 ? cmp : a.compareToIgnoreCase(b);
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

    private static void safeSetPlayerListName(Player player, String name) {
        if (player == null) return;
        try {
            if (name != null && name.length() <= 16) {
                player.setPlayerListName(name);
            } else {
                player.setPlayerListName(player.getName());
            }
        } catch (Throwable ignored) {
        }
    }
}