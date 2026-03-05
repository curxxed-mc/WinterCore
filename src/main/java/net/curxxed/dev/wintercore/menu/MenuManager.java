package net.curxxed.dev.wintercore.menu;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager {

    private static MenuManager instance;

    private final WinterCore plugin;
    private final Map<UUID, Menu> openMenus = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();

    private MenuManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public static void initialize(WinterCore plugin) {
        if (instance == null) {
            instance = new MenuManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(new MenuListener(instance), plugin);
        }
    }

    public static MenuManager getInstance() {
        if (instance == null) throw new IllegalStateException("MenuManager has not been initialized.");
        return instance;
    }

    public void openMenu(Player player, Menu menu) {
        UUID uuid = player.getUniqueId();
        Inventory inventory = menu.buildInventory(player);

        openMenus.put(uuid, menu);
        openInventories.put(uuid, inventory);

        player.openInventory(inventory);
        menu.onOpen(player);
    }

    public void refreshMenu(Player player, Menu menu) {
        Inventory inventory = menu.buildInventory(player);
        player.getOpenInventory().getTopInventory().setContents(inventory.getContents());
    }

    public Menu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }

    public void closeMenu(Player player, Inventory closedInventory) {
        UUID uuid = player.getUniqueId();
        Inventory registered = openInventories.get(uuid);
        if (registered == null || !registered.equals(closedInventory)) return;

        openInventories.remove(uuid);
        Menu menu = openMenus.remove(uuid);
        if (menu != null) menu.onClose(player);
    }

    public boolean hasOpenMenu(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }
}