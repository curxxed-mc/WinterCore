package net.curxxed.dev.wintercore.nametags;

import net.curxxed.dev.wintercore.plugin.WinterCore;
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

    private static final String TEAM_PREFIX = "wc_";
    private static final String FALLBACK_RANK = "Default";
    private static final String FALLBACK_COLOR = "&f";

    private final WinterCore plugin;

    private final Map<UUID, Scoreboard> viewerBoards = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerRankCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> colorOverrides = new ConcurrentHashMap<>();

    private volatile Map<String, String> rankTeamKey = new HashMap<>();
    private final Set<UUID> staffModePlayers = ConcurrentHashMap.newKeySet();

    public NameTagColorManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rebuildRankIndex();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player viewer : Utilities.getOnlinePlayers()) {
            viewerBoards.put(viewer.getUniqueId(), createBoardForViewer(viewer));
        }
    }

    public void unload() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Utilities.getOnlinePlayers()) {
            try { p.setScoreboard(main); } catch (Exception ignored) {}
        }
        viewerBoards.clear();
        playerRankCache.clear();
        colorOverrides.clear();
    }

    public void applyRank(Player player, String rankName) {
        colorOverrides.remove(player.getUniqueId());
        playerRankCache.put(player.getUniqueId(), rankName);
        scheduleRefresh(player);
    }

    public void applyColor(Player player, String color) {
        colorOverrides.put(player.getUniqueId(), color != null ? color : FALLBACK_COLOR);
        scheduleRefresh(player);
    }

    public void setStaffMode(Player player, boolean active) {
        if (active) {
            staffModePlayers.add(player.getUniqueId());
        } else {
            staffModePlayers.remove(player.getUniqueId());
        }
        scheduleRefreshWithStaffTag(player);
    }

    public void refresh(Player target) {
        scheduleRefresh(target);
    }

    public void fullRefresh() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            rebuildRankIndex();
            List<Player> online = Utilities.getOnlinePlayers();
            for (Scoreboard board : viewerBoards.values()) {
                for (Player target : online) {
                    applyToBoard(board, target);
                }
            }
        });
    }

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
        UUID uuid = leaving.getUniqueId();
        String entryName = leaving.getName();

        viewerBoards.remove(uuid);
        playerRankCache.remove(uuid);
        colorOverrides.remove(uuid);
        staffModePlayers.remove(uuid);

        for (Scoreboard board : viewerBoards.values()) {
            removeEntryFromBoard(board, entryName);
        }
    }

    private Scoreboard createBoardForViewer(Player viewer) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        for (Player target : Utilities.getOnlinePlayers()) {
            applyToBoard(board, target);
        }
        viewer.setScoreboard(board);
        return board;
    }

    private void applyToBoard(Scoreboard board, Player target) {
        String rankName = playerRankCache.getOrDefault(target.getUniqueId(), FALLBACK_RANK);
        String key = rankTeamKey.getOrDefault(rankName, TEAM_PREFIX + "99");
        String color = colorOverrides.containsKey(target.getUniqueId())
                ? colorOverrides.get(target.getUniqueId())
                : getRankColor(rankName);
        String entry = target.getName();

        Team team = board.getTeam(key);
        if (team == null) team = board.registerNewTeam(key);
        NameTagVersionHelper.applyColor(team, color);

        for (Team t : board.getTeams()) {
            if (!t.getName().equals(key) && t.hasEntry(entry)) {
                t.removeEntry(entry);
            }
        }

        if (!team.hasEntry(entry)) team.addEntry(entry);
    }

    private void sendStaffModeTag(Player viewer, Player target, String color) {
        String translatedColor = net.curxxed.dev.wintercore.utils.CC.translate(color);
        String teamName = TEAM_PREFIX + rankTeamKey.getOrDefault(
                playerRankCache.getOrDefault(target.getUniqueId(), FALLBACK_RANK), "99");
        String longPrefix = net.curxxed.dev.wintercore.utils.CC.translate("&7*Staff Mode*") + "\n" + translatedColor;

        try {
            Object packet = Utilities.getNMSClass("PacketPlayOutScoreboardTeam").newInstance();
            Utilities.setField(packet, "a", teamName);
            Utilities.setField(packet, "b", teamName);
            Utilities.setField(packet, "c", longPrefix);
            Utilities.setField(packet, "d", "");
            Utilities.setField(packet, "e", "always");
            Utilities.setField(packet, "f", "always");
            Utilities.setField(packet, "g", 0);
            Utilities.setField(packet, "h", java.util.Collections.emptyList());
            Utilities.setField(packet, "i", 2);
            Utilities.sendPacket(viewer, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send staff mode tag packet: " + e.getMessage());
        }
    }

    private void scheduleRefreshWithStaffTag(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String rankName = playerRankCache.getOrDefault(target.getUniqueId(), FALLBACK_RANK);
            String color = colorOverrides.containsKey(target.getUniqueId())
                    ? colorOverrides.get(target.getUniqueId())
                    : getRankColor(rankName);
            boolean inStaffMode = staffModePlayers.contains(target.getUniqueId());

            for (Map.Entry<UUID, Scoreboard> entry : viewerBoards.entrySet()) {
                Scoreboard board = entry.getValue();
                applyToBoard(board, target);
                if (inStaffMode) {
                    Player viewer = Bukkit.getPlayer(entry.getKey());
                    if (viewer != null && viewer.isOnline()) {
                        sendStaffModeTag(viewer, target, color);
                    }
                }
            }
        });
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
            rankTeamKey = new HashMap<>();
            return;
        }

        List<String> rankNames = new ArrayList<>(section.getKeys(false));
        rankNames.sort((a, b) -> {
            int wa = section.getInt(a + ".weight", 0);
            int wb = section.getInt(b + ".weight", 0);
            int cmp = Integer.compare(wb, wa);
            return cmp != 0 ? cmp : a.compareTo(b);
        });

        Map<String, String> built = new HashMap<>();
        for (int i = 0; i < rankNames.size(); i++) {
            built.put(rankNames.get(i), TEAM_PREFIX + String.format("%02d", i));
        }
        rankTeamKey = Collections.unmodifiableMap(built);
    }

    private void scheduleRefresh(Player target) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Scoreboard board : viewerBoards.values()) {
                applyToBoard(board, target);
            }
        });
    }

    private String getRankColor(String rankName) {
        ConfigurationSection section = plugin.getRankManager().getRanksSection();
        if (section == null) return FALLBACK_COLOR;
        return section.getString(rankName + ".name-color", FALLBACK_COLOR);
    }
}