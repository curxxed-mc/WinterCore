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

    WinterCoreListeners register() {
        PluginManager pm = plugin.getServer().getPluginManager();

        PlayerService playerService = new PlayerService(plugin);
        MessagingService messagingService = new MessagingService(plugin, playerService);
        StaffChatService staffChatService = new StaffChatService(plugin);
        ChatListener chatListener = new ChatListener(plugin, plugin.getTagsManager(), playerService, staffChatService);
        FreezeListener freezeListener = new FreezeListener(playerService, plugin);
        BanList banList = new BanList(plugin);

        pm.registerEvents(playerService, plugin);
        pm.registerEvents(chatListener, plugin);
        pm.registerEvents(new ConnectionListener(plugin, plugin.getDisguiseEventListener(), plugin.getNRS()), plugin);
        pm.registerEvents(freezeListener, plugin);
        pm.registerEvents(new RankMenu.ChatListener(plugin), plugin);
        pm.registerEvents(plugin.getSocialInput(), plugin);
        pm.registerEvents(new StaffModeListener(plugin, plugin.getStaffModeManager()), plugin);
        pm.registerEvents(plugin.getDisguiseEventListener(), plugin);
        pm.registerEvents(banList, plugin);

        return new WinterCoreListeners(
                playerService,
                messagingService,
                staffChatService,
                chatListener,
                freezeListener,
                banList
        );
    }
}
