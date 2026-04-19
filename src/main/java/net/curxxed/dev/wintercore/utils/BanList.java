package net.curxxed.dev.wintercore.utils;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BanList implements Listener {

    private final WinterCore plugin;
    private final DatabaseManager databaseManager;
    private final Set<UUID> bannedCache = ConcurrentHashMap.newKeySet();

    public BanList(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        databaseManager.getModerationService().isPlayerBanned(uuid, isBanned -> {
            if (isBanned) {
                bannedCache.add(uuid);
            } else {
                bannedCache.remove(uuid);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!bannedCache.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        player.sendMessage(CC.translate("&cYou are banned and cannot use commands."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bannedCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChatEvent(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (!bannedCache.contains(p.getUniqueId())) return;
        event.setCancelled(true);
        p.sendMessage(CC.translate("&cYou are banned and cannot chat."));

    }
}