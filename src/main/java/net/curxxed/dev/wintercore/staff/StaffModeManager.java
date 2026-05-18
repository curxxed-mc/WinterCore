package net.curxxed.dev.wintercore.staff;

import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class StaffModeManager {

    private static final Material PUSH_FORWARD_MATERIAL = Material.FEATHER;
    private static final double TARGET_RANGE_SQUARED = 25.0D;

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private final Set<UUID> staffModePlayers = new HashSet<>();
    private final WinterCore plugin;

    public StaffModeManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public boolean hasStaffPermission(Player player) {
        return player.hasPermission("wintercore.staff")
                || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager")
                || player.isOp();
    }

    public boolean isInStaffMode(Player player) {
        return staffModePlayers.contains(player.getUniqueId());
    }

    public void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isInStaffMode(player)) {
            savedInventories.put(uuid, cloneContents(player.getInventory().getContents()));
            savedGameModes.put(uuid, player.getGameMode());
        }

        staffModePlayers.add(uuid);
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();

        player.getInventory().setItem(0, createItem(PUSH_FORWARD_MATERIAL,
                itemName("push-forward", "&6Push Forward"),
                itemLore("push-forward", Collections.singletonList("&7Use this to move forward quickly."))));
        player.getInventory().setItem(1, createItem(Material.SKULL_ITEM,
                itemName("staff-list", "&eStaff List"),
                itemLore("staff-list", Collections.singletonList("&7View the list of online staff members."))));
        player.getInventory().setItem(2, createItem(Material.NETHER_STAR,
                itemName("random-teleport", "&bRandom Teleport"),
                itemLore("random-teleport", Collections.singletonList("&7Teleport to a random player."))));
        player.getInventory().setItem(4, createItem(Material.CARPET,
                itemName("better-view", "&bBetter View"),
                itemLore("better-view", Collections.singletonList("&7Enhance your view for better observation."))));
        player.getInventory().setItem(6, createItem(Material.BOOK,
                itemName("inspect-player", "&bInspect Player"),
                itemLore("inspect-player", Collections.singletonList("&7Inspect a nearby player's inventory."))));
        player.getInventory().setItem(7, createItem(Material.PACKED_ICE,
                itemName("freeze-player", "&bFreeze Player"),
                itemLore("freeze-player", Collections.singletonList("&7Freeze a nearby player."))));

        updateVanishItem(player);
        refreshNameTag(player, true);
        player.sendMessage(message("staff-mode.enabled", "&aStaff mode enabled."));
    }

    public void disableStaffMode(Player player) {
        disableStaffMode(player, true);
    }

    public void handleQuit(Player player) {
        disableStaffMode(player, false);
    }

    private void disableStaffMode(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        boolean wasInStaffMode = staffModePlayers.remove(uuid);
        ItemStack[] savedInventory = savedInventories.remove(uuid);
        GameMode savedGameMode = savedGameModes.remove(uuid);

        if (!wasInStaffMode && savedInventory == null && savedGameMode == null) {
            return;
        }

        player.getInventory().clear();

        if (savedInventory != null) {
            player.getInventory().setContents(cloneContents(savedInventory));
        }

        player.setGameMode(savedGameMode != null ? savedGameMode : GameMode.SURVIVAL);

        refreshNameTag(player, false);
        if (notify) {
            player.sendMessage(message("staff-mode.disabled", "&cStaff mode disabled and inventory restored."));
        }
    }

    public void handleItemUse(Player player, String itemName, Player target, ItemStack item) {
        if (itemName == null) return;

        if (itemName.equals(itemName("push-forward", "&6Push Forward"))) {
            player.setVelocity(player.getLocation().getDirection().multiply(3.5));

        } else if (itemName.equals(itemName("staff-list", "&eStaff List"))) {
            player.performCommand("stafflist");

        } else if (itemName.equals(itemName("random-teleport", "&bRandom Teleport"))) {
            teleportToRandomPlayer(player);

        } else if (itemName.equals(itemName("inspect-player", "&bInspect Player"))) {
            if (target != null) player.performCommand("invsee " + target.getName());
            else player.sendMessage(message("staff-mode.no-nearby-inspect", "&cNo player nearby to inspect."));

        } else if (itemName.equals(itemName("freeze-player", "&bFreeze Player"))) {
            if (target != null) player.performCommand("freeze " + target.getName());
            else player.sendMessage(message("staff-mode.no-nearby-freeze", "&cNo player nearby to freeze."));

        } else if (itemName.equals(vanishItemName(false)) || itemName.equals(vanishItemName(true))) {
            VanishCommand.toggleVanish(player, plugin, vanished -> updateVanishItem(player));
        }
    }

    private void refreshNameTag(Player player, boolean staffModeActive) {
        if (plugin.getNameTagColorManager() != null) {
            plugin.getNameTagColorManager().setStaffMode(player, staffModeActive);
        }
    }

    private void teleportToRandomPlayer(Player player) {
        List<Player> nonStaff = new ArrayList<>();
        for (Player p : net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers()) {
            if (!hasStaffPermission(p)) nonStaff.add(p);
        }

        if (nonStaff.isEmpty()) {
            player.sendMessage(message("staff-mode.no-random-targets", "&cNo non-staff players online."));
            return;
        }

        Player target = nonStaff.get(ThreadLocalRandom.current().nextInt(nonStaff.size()));
        player.teleport(target);
        player.sendMessage(message("staff-mode.random-teleport-success", "&aTeleported to {target}.",
                "{target}", target.getName()));
    }

    private void updateVanishItem(Player player) {
        boolean vanished = VanishCommand.isVanished(player);
        player.getInventory().setItem(8, createItem(
                Material.INK_SACK,
                vanishItemName(vanished),
                (short) (vanished ? 8 : 10),
                plugin.getMessageConfig().getList("vanish.item.lore",
                        Collections.singletonList("&7Toggle vanish mode."))
        ));
    }

    public Player getTargetPlayer(Player player) {
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        Location origin = player.getLocation();

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || !player.canSee(other)) continue;
            double dist = origin.distanceSquared(other.getLocation());
            if (dist <= TARGET_RANGE_SQUARED && dist < closestDist) {
                closestDist = dist;
                closest = other;
            }
        }
        return closest;
    }

    public boolean isStaffModeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }

        String displayName = item.getItemMeta().getDisplayName();
        return displayName.equals(itemName("push-forward", "&6Push Forward"))
                || displayName.equals(itemName("staff-list", "&eStaff List"))
                || displayName.equals(itemName("random-teleport", "&bRandom Teleport"))
                || displayName.equals(itemName("better-view", "&bBetter View"))
                || displayName.equals(itemName("inspect-player", "&bInspect Player"))
                || displayName.equals(itemName("freeze-player", "&bFreeze Player"))
                || displayName.equals(vanishItemName(false))
                || displayName.equals(vanishItemName(true));
    }

    private String itemName(String key, String fallback) {
        return message("staff-mode.items." + key + ".name", fallback);
    }

    private List<String> itemLore(String key, List<String> fallback) {
        return plugin.getMessageConfig().getList("staff-mode.items." + key + ".lore", fallback);
    }

    private String vanishItemName(boolean vanished) {
        return message(vanished ? "vanish.item.unvanish" : "vanish.item.vanish",
                vanished ? "&7Un-Vanish" : "&aVanish");
    }

    private String message(String path, String fallback, String... placeholders) {
        return plugin.getMessageConfig().get(path, fallback, placeholders);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, (short) 0, lore);
    }

    private ItemStack createItem(Material material, String name, short data, List<String> lore) {
        ItemBuilder itemBuilder = new ItemBuilder(material, 1, data);
        itemBuilder.setName(name);
        if (lore != null && !lore.isEmpty()) {
            itemBuilder.setLore(lore);
        }
        return itemBuilder.toItemStack();
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }
}
