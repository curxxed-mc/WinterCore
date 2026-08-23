package net.curxxed.dev.wintercore.plugin;

import net.curxxed.dev.wintercore.listeners.ConnectionListener;
import net.curxxed.dev.wintercore.menus.RankMenu;
import net.curxxed.dev.wintercore.staff.StaffModeListener;
import org.bukkit.plugin.PluginManager;

final class WinterCoreListenerRegistrar {

    private final WinterCore plugin;

    WinterCoreListenerRegistrar(WinterCore plugin) {
        this.plugin = plugin;
    }

    void register() {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(plugin.getPlayerService(), plugin);
        pm.registerEvents(plugin.getChatListener(), plugin);
        pm.registerEvents(new ConnectionListener(plugin, plugin.getDisguiseEventListener(), plugin.getNetworkRedisService()), plugin);
        pm.registerEvents(plugin.getFreezeListener(), plugin);
        pm.registerEvents(plugin.getVanishService(), plugin);
        pm.registerEvents(new RankMenu.ChatListener(plugin), plugin);
        pm.registerEvents(plugin.getSocialInput(), plugin);
        pm.registerEvents(new StaffModeListener(plugin, plugin.getStaffModeManager()), plugin);
        pm.registerEvents(plugin.getDisguiseEventListener(), plugin);
        pm.registerEvents(plugin.getBanList(), plugin);
    }
}
