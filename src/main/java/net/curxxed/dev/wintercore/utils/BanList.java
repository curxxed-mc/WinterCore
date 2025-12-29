package net.curxxed.dev.wintercore.utils;

import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class BanList implements Listener {
    private final WinterCore plugin;
    private final DatabaseManager databaseManager;

    public BanList(WinterCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        databaseManager.isPlayerBanned(uuid, isBanned -> {
            if (isBanned) {
                event.setCancelled(true);
                player.sendMessage(CC.translate("&cYou are banned and cannot use commands."));
            }
        });
    }
}
