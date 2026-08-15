package net.curxxed.dev.wintercore.nametags;

import org.bukkit.ChatColor;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.player.WinterCorePlayer;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagColorManager implements Listener {

    private static final long BOARD_SYNC_INTERVAL_TICKS = 20L;
    private static final String TEAM_PREFIX = "wc_";
    private static final String FALLBACK_RANK = "Default";
    private static final String FALLBACK_COLOR = "&f";
    private static final String STAFF_PREFIX = CC.translate("&7*");

    private final WinterCore plugin;
    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();
    private final Map<UUID, String> rankCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorOverrides = new ConcurrentHashMap<>();
    private final Map<UUID, String> disguiseNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastAppliedEntries = new ConcurrentHashMap<>();
    private final Map<TeamReference, ExternalTeamSnapshot> externalTeamSnapshots = new ConcurrentHashMap<>();
    private final Set<UUID> staffMode = ConcurrentHashMap.newKeySet();

    private volatile Map<String, String> rankTeamKey = Collections.emptyMap();
    private BukkitTask boardSyncTask;

    public NameTagColorManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rebuildRankIndex();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player p : Utilities.getOnlinePlayers()) {
            viewerBoards.put(p.getUniqueId(), getViewerBoard(p));
            updatePlayerListName(p);
            lastAppliedEntries.put(p.getUniqueId(), getVisibleName(p));
        }

        startBoardWatcher();
        fullRefresh();
    }

    public void unload() {
        if (boardSyncTask != null) {
            boardSyncTask.cancel();
            boardSyncTask = null;
        }

        restoreExternalTeams();
        clearWinterCoreTeams();

        for (Player p : Utilities.getOnlinePlayers()) {
            try {
                safeSetPlayerListName(p, p.getName());
            } catch (Exception ignored) {
            }
        }

        viewerBoards.clear();
        rankCache.clear();
        colorOverrides.clear();
        disguiseNames.clear();
        lastAppliedEntries.clear();
        externalTeamSnapshots.clear();
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
        plugin.getTasks().sync(() -> {
            rebuildRankIndex();
            syncViewerBoards();
            for (Player target : Utilities.getOnlinePlayers()) {
                updatePlayerListName(target);
                refreshAllViewersForTarget(target);
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
        viewerBoards.put(joining.getUniqueId(), getViewerBoard(joining));
        updatePlayerListName(joining);
        lastAppliedEntries.put(joining.getUniqueId(), getVisibleName(joining));
        applyAllToBoard(getViewerBoard(joining));
        refreshAllViewersForTarget(joining);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        UUID uuid = leaving.getUniqueId();
        String name = leaving.getName();
        String visibleName = lastAppliedEntries.remove(uuid);

        viewerBoards.remove(uuid);
        rankCache.remove(uuid);
        colorOverrides.remove(uuid);
        staffMode.remove(uuid);
        disguiseNames.remove(uuid);
        pruneStaleExternalSnapshots();

        removeEntryFromAllBoards(name);
        if (visibleName != null && !visibleName.equals(name)) {
            removeEntryFromAllBoards(visibleName);
        }
    }

    private void applyToBoard(Scoreboard board, Player target, String entry) {
        if (board == null || target == null || !target.isOnline() || entry == null || entry.isEmpty()) {
            return;
        }

        UUID uuid = target.getUniqueId();
        String rankName = resolveRankName(target);
        String teamKey = rankTeamKey.getOrDefault(rankName, TEAM_PREFIX + "99");
        String colorPrefix = resolveColorPrefix(uuid, rankName);

        Team existingTeam = findEntryTeam(board, entry);
        if (existingTeam != null
                && isWinterCoreTeam(existingTeam)
                && !existingTeam.getName().equals(teamKey)) {
            existingTeam.removeEntry(entry);
            existingTeam = null;
        }

        if (existingTeam != null && !isWinterCoreTeam(existingTeam)) {
            if (!canInjectIntoExistingTeam(existingTeam, entry)) {
                return;
            }
            applyToExternalTeam(board, existingTeam, uuid, colorPrefix);
            return;
        }

        Team team = board.getTeam(teamKey);
        if (team == null) {
            team = board.registerNewTeam(teamKey);
        }

        removeEntryFromOtherWinterCoreTeams(board, teamKey, entry);
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }

        team.setPrefix(buildManagedPrefix(uuid, colorPrefix));
        team.setSuffix("");

        if (NameTagVersionHelper.HAS_TEAM_COLOR_API) {
            ChatColor color = staffMode.contains(uuid)
                    ? ChatColor.WHITE
                    : NameTagVersionHelper.extractPrimaryColor(colorPrefix);
            setTeamColor(team, color != null ? color : ChatColor.WHITE);
        }
    }

    private void scheduleRefresh(Player target) {
        plugin.getTasks().sync(() -> {
            updatePlayerListName(target);
            refreshAllViewersForTarget(target);
        });
    }

    private void updatePlayerListName(Player player) {
        if (player == null || !player.isOnline()) return;
        String color = resolveColorPrefix(player.getUniqueId(), resolveRankName(player));
        safeSetPlayerListName(player, color + getVisibleName(player));
    }

    private void refreshAllViewersForTarget(Player target) {
        String entry = getVisibleName(target);
        removeStaleEntries(target.getUniqueId(), entry);
        for (Scoreboard board : collectActiveBoards()) {
            applyToBoard(board, target, entry);
        }
        lastAppliedEntries.put(target.getUniqueId(), entry);
    }

    private void removeEntryFromBoard(Scoreboard board, String entryName) {
        for (Team team : board.getTeams()) {
            if (team.hasEntry(entryName)) {
                team.removeEntry(entryName);
            }
        }
    }

    private void removeEntryFromAllBoards(String entryName) {
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        for (Scoreboard board : collectActiveBoards()) {
            removeEntryFromBoard(board, entryName);
        }
    }

    private void removeEntryFromOtherWinterCoreTeams(Scoreboard board, String teamKey, String entryName) {
        for (Team team : board.getTeams()) {
            if (isWinterCoreTeam(team)
                    && !team.getName().equals(teamKey)
                    && team.hasEntry(entryName)) {
                team.removeEntry(entryName);
            }
        }
    }

    private void removeStaleEntries(UUID uuid, String currentEntry) {
        String previousEntry = lastAppliedEntries.get(uuid);
        if (previousEntry != null && !previousEntry.equals(currentEntry)) {
            removeEntryFromAllBoards(previousEntry);
        }
    }

    private void applyAllToBoard(Scoreboard board) {
        if (board == null) {
            return;
        }
        for (Player target : Utilities.getOnlinePlayers()) {
            applyToBoard(board, target, getVisibleName(target));
        }
    }

    private List<Scoreboard> collectActiveBoards() {
        List<Scoreboard> boards = new ArrayList<>();
        Map<Scoreboard, Boolean> seen = new IdentityHashMap<>();
        for (Player viewer : Utilities.getOnlinePlayers()) {
            Scoreboard board = getViewerBoard(viewer);
            if (board != null && seen.put(board, Boolean.TRUE) == null) {
                boards.add(board);
            }
        }
        return boards;
    }

    private List<Scoreboard> collectTrackedBoards() {
        List<Scoreboard> boards = new ArrayList<>();
        Map<Scoreboard, Boolean> seen = new IdentityHashMap<>();
        for (Scoreboard board : viewerBoards.values()) {
            if (board != null && seen.put(board, Boolean.TRUE) == null) {
                boards.add(board);
            }
        }
        for (Player viewer : Utilities.getOnlinePlayers()) {
            Scoreboard board = getViewerBoard(viewer);
            if (board != null && seen.put(board, Boolean.TRUE) == null) {
                boards.add(board);
            }
        }
        return boards;
    }

    private void startBoardWatcher() {
        boardSyncTask = plugin.getTasks().timer( this::syncViewerBoards, 1L, BOARD_SYNC_INTERVAL_TICKS);
    }

    private void syncViewerBoards() {
        for (Player viewer : Utilities.getOnlinePlayers()) {
            Scoreboard currentBoard = getViewerBoard(viewer);
            Scoreboard previousBoard = viewerBoards.put(viewer.getUniqueId(), currentBoard);
            if (currentBoard != null && currentBoard != previousBoard) {
                applyAllToBoard(currentBoard);
            }
        }
        viewerBoards.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        pruneStaleExternalSnapshots();
    }

    private void clearWinterCoreTeams() {
        for (Scoreboard board : collectTrackedBoards()) {
            for (Team team : new ArrayList<>(board.getTeams())) {
                if (isWinterCoreTeam(team)) {
                    try {
                        team.unregister();
                    } catch (IllegalStateException ignored) {
                    }
                }
            }
        }
    }

    private void restoreExternalTeams() {
        for (Map.Entry<TeamReference, ExternalTeamSnapshot> entry : externalTeamSnapshots.entrySet()) {
            Team team = entry.getKey().resolveTeam();
            ExternalTeamSnapshot snapshot = entry.getValue();
            if (team == null || !snapshot.matchesAppliedState(team)) {
                continue;
            }

            team.setPrefix(snapshot.getBasePrefix());
            team.setSuffix(snapshot.getBaseSuffix());
            if (NameTagVersionHelper.HAS_TEAM_COLOR_API) {
                setTeamColor(team, snapshot.getBaseColor() != null ? snapshot.getBaseColor() : ChatColor.WHITE);
            }
        }
    }

    private void pruneStaleExternalSnapshots() {
        externalTeamSnapshots.entrySet().removeIf(entry -> entry.getKey().resolveTeam() == null);
    }

    private Scoreboard getViewerBoard(Player viewer) {
        if (viewer == null) {
            return Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard()
                    : null;
        }

        Scoreboard board = viewer.getScoreboard();
        if (board != null) {
            return board;
        }

        return Bukkit.getScoreboardManager() != null
                ? Bukkit.getScoreboardManager().getMainScoreboard()
                : null;
    }

    private Team findEntryTeam(Scoreboard board, String entry) {
        for (Team team : board.getTeams()) {
            if (team.hasEntry(entry)) {
                return team;
            }
        }
        return null;
    }

    private void applyToExternalTeam(Scoreboard board, Team team, UUID uuid, String colorPrefix) {
        TeamReference reference = new TeamReference(board, team.getName());
        ExternalTeamSnapshot snapshot = externalTeamSnapshots.computeIfAbsent(reference, key -> ExternalTeamSnapshot.capture(team));
        snapshot.refreshBaseIfChanged(team);

        String externalPrefix = buildExternalPrefix(snapshot.getBasePrefix(), uuid, colorPrefix);
        if (externalPrefix == null) {
            return;
        }

        team.setPrefix(externalPrefix);
        team.setSuffix(snapshot.getBaseSuffix());

        if (NameTagVersionHelper.HAS_TEAM_COLOR_API) {
            ChatColor color = staffMode.contains(uuid)
                    ? (snapshot.getBaseColor() != null ? snapshot.getBaseColor() : ChatColor.WHITE)
                    : NameTagVersionHelper.extractPrimaryColor(colorPrefix);
            setTeamColor(team, color != null ? color : ChatColor.WHITE);
        }

        snapshot.markApplied(team);
    }

    private String buildManagedPrefix(UUID uuid, String colorPrefix) {
        if (staffMode.contains(uuid)) {
            return trimToTeamLength(STAFF_PREFIX);
        }
        return trimToTeamLength(colorPrefix);
    }

    private String buildExternalPrefix(String basePrefix, UUID uuid, String colorPrefix) {
        String safeBase = basePrefix != null ? basePrefix : "";
        String addition;
        if (staffMode.contains(uuid)) {
            addition = STAFF_PREFIX;
        } else if (NameTagVersionHelper.HAS_TEAM_COLOR_API) {
            addition = "";
        } else {
            addition = colorPrefix;
        }

        if (safeBase.length() + addition.length() > 16) {
            return null;
        }

        return safeBase + addition;
    }

    private String resolveRankName(Player player) {
        UUID uuid = player.getUniqueId();
        String cachedRank = rankCache.get(uuid);
        if (cachedRank != null && !cachedRank.trim().isEmpty()) {
            return cachedRank;
        }

        if (plugin.getPlayerService() != null) {
            WinterCorePlayer data = plugin.getPlayerService().getPlayerData(uuid);
            if (data != null && data.getRank() != null && !data.getRank().trim().isEmpty()) {
                rankCache.put(uuid, data.getRank());
                return data.getRank();
            }
        }

        if (plugin.getRankManager() != null) {
            String resolved = plugin.getRankManager().getRankSync(player);
            if (resolved != null && !resolved.trim().isEmpty()) {
                rankCache.put(uuid, resolved);
                return resolved;
            }
        }

        return FALLBACK_RANK;
    }

    private String resolveColorPrefix(UUID uuid, String rankName) {
        String rawColor = colorOverrides.containsKey(uuid)
                ? colorOverrides.get(uuid)
                : getRankColor(rankName);
        return trimToTeamLength(CC.translate(rawColor != null ? rawColor : FALLBACK_COLOR));
    }

    private static String trimToTeamLength(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() > 16 ? value.substring(0, 16) : value;
    }

    private static boolean canInjectIntoExistingTeam(Team team, String entry) {
        return team.hasEntry(entry) && team.getEntries().size() == 1;
    }

    private static boolean isWinterCoreTeam(Team team) {
        return team != null && team.getName().startsWith(TEAM_PREFIX);
    }

    private static void setTeamColor(Team team, ChatColor color) {
        if (!NameTagVersionHelper.HAS_TEAM_COLOR_API || color == null) {
            return;
        }
        try {
            Team.class.getMethod("setColor", ChatColor.class).invoke(team, color);
        } catch (Exception ignored) {
        }
    }

    private static ChatColor getTeamColor(Team team) {
        if (!NameTagVersionHelper.HAS_TEAM_COLOR_API || team == null) {
            return null;
        }
        try {
            Object value = Team.class.getMethod("getColor").invoke(team);
            return value instanceof ChatColor ? (ChatColor) value : null;
        } catch (Exception ignored) {
            return null;
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
            if (name != null && (Utilities.IS_1_13_OR_NEWER || name.length() <= 16)) {
                player.setPlayerListName(name);
            } else {
                player.setPlayerListName(player.getName());
            }
        } catch (Throwable ignored) {
        }
    }

    private static final class TeamReference {
        private final Scoreboard board;
        private final String teamName;

        private TeamReference(Scoreboard board, String teamName) {
            this.board = board;
            this.teamName = teamName;
        }

        private Team resolveTeam() {
            return board != null ? board.getTeam(teamName) : null;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TeamReference)) {
                return false;
            }
            TeamReference other = (TeamReference) obj;
            return board == other.board && Objects.equals(teamName, other.teamName);
        }

        @Override
        public int hashCode() {
            return (31 * System.identityHashCode(board)) + teamName.hashCode();
        }
    }

    private static final class ExternalTeamSnapshot {
        private String basePrefix;
        private String baseSuffix;
        private ChatColor baseColor;
        private String appliedPrefix;
        private String appliedSuffix;
        private ChatColor appliedColor;

        private static ExternalTeamSnapshot capture(Team team) {
            ExternalTeamSnapshot snapshot = new ExternalTeamSnapshot();
            snapshot.basePrefix = team.getPrefix();
            snapshot.baseSuffix = team.getSuffix();
            snapshot.baseColor = getTeamColor(team);
            return snapshot;
        }

        private void refreshBaseIfChanged(Team team) {
            String currentPrefix = team.getPrefix();
            String currentSuffix = team.getSuffix();
            ChatColor currentColor = getTeamColor(team);
            if (!Objects.equals(currentPrefix, appliedPrefix)
                    || !Objects.equals(currentSuffix, appliedSuffix)
                    || !Objects.equals(currentColor, appliedColor)) {
                basePrefix = currentPrefix;
                baseSuffix = currentSuffix;
                baseColor = currentColor;
            }
        }

        private void markApplied(Team team) {
            appliedPrefix = team.getPrefix();
            appliedSuffix = team.getSuffix();
            appliedColor = getTeamColor(team);
        }

        private boolean matchesAppliedState(Team team) {
            return Objects.equals(team.getPrefix(), appliedPrefix)
                    && Objects.equals(team.getSuffix(), appliedSuffix)
                    && Objects.equals(getTeamColor(team), appliedColor);
        }

        private String getBasePrefix() {
            return basePrefix != null ? basePrefix : "";
        }

        private String getBaseSuffix() {
            return baseSuffix != null ? baseSuffix : "";
        }

        private ChatColor getBaseColor() {
            return baseColor;
        }
    }
}
