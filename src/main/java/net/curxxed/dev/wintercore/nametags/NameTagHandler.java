package net.curxxed.dev.wintercore.nametags;

import lombok.Getter;
import lombok.Setter;
import net.curxxed.dev.wintercore.managers.Handler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class NameTagHandler extends Handler implements Listener {
    private Map<UUID, NameTagBoard> nameTagsData = new HashMap<>();
    private final NameTagAdapter nameTagAdapter;
    private BukkitTask refreshTaskId = null;

    public NameTagHandler(WinterCore plugin) {
        super(plugin);
        this.nameTagAdapter = new DefaultNameTagAdapter(plugin);
    }

    public void load() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
        Utilities.getOnlinePlayers().forEach(player -> this.nameTagsData.putIfAbsent(player.getUniqueId(), new NameTagBoard(this.plugin, player.getUniqueId())));
    }

    public void updateNameTagFor(Player target) {
        String color = plugin.getDisguiseRegistry().getCachedColor(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            String teamName = ("wcd_" + target.getName()).substring(0, Math.min(16, ("wcd_" + target.getName()).length()));
            Scoreboard board = viewer.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();
                viewer.setScoreboard(board);
            }
            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            team.setPrefix(ChatColor.translateAlternateColorCodes('&', color));
            team.setSuffix("");
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }
        target.setPlayerListName(ChatColor.translateAlternateColorCodes('&', color) + target.getName() + ChatColor.RESET);
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.nameTagsData.putIfAbsent(player.getUniqueId(), new NameTagBoard(this.plugin, player.getUniqueId()));
        plugin.getDisguiseRegistry().updateColorCache(player);
        Bukkit.getOnlinePlayers().forEach(viewer -> updateNameTagFor(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.nameTagsData.remove(player.getUniqueId());
        plugin.getDisguiseRegistry().removeColorCache(player.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            String teamName = ("wcd_" + player.getName()).substring(0, Math.min(16, ("wcd_" + player.getName()).length()));
            Scoreboard board = viewer.getScoreboard();
            Team team = board.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
    }

    public void clear() {
        if (refreshTaskId != null) {
            refreshTaskId.cancel();
            refreshTaskId = null;
        }
        Iterator<UUID> tags = this.nameTagsData.keySet().iterator();

        while (tags.hasNext()) {
            UUID uuid = tags.next();

            Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                tags.remove();
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }
    }
}
