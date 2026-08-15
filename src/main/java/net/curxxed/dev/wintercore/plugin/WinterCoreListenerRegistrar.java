package net.curxxed.dev.wintercore.plugin;

import net.curxxed.dev.wintercore.chat.ChatListener;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.listeners.ConnectionListener;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.player.BanList;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.staff.StaffModeListener;
import org.bukkit.plugin.PluginManager;

final class WinterCoreListenerRegistrar {

    private final WinterCore plugin;

    WinterCoreListenerRegistrar(WinterCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        PluginManager pm = plugin.getServer().getPluginManager();

        plugin.playerService = new PlayerService(plugin);
        plugin.messagingService = new MessagingService(plugin, plugin.playerService);
        plugin.staffChatService = new StaffChatService(plugin);
        plugin.chatListener = new ChatListener(plugin, plugin.getTagsManager(), plugin.playerService, plugin.staffChatService);
        plugin.freezeListener = new FreezeListener(plugin.playerService, plugin);
        plugin.banList = new BanList(plugin);

        pm.registerEvents(plugin.playerService, plugin);
        pm.registerEvents(plugin.chatListener, plugin);
        pm.registerEvents(new ConnectionListener(plugin, plugin.getDisguiseEventListener(), plugin.getNRS()), plugin);
        pm.registerEvents(plugin.freezeListener, plugin);
        pm.registerEvents(new RankMenu.ChatListener(plugin), plugin);
        pm.registerEvents(plugin.getSocialInput(), plugin);
        pm.registerEvents(new StaffModeListener(plugin, plugin.getStaffModeManager()), plugin);
        pm.registerEvents(plugin.getDisguiseEventListener(), plugin);
        pm.registerEvents(plugin.banList, plugin);
    }
}
