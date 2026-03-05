package net.curxxed.dev.wintercore.staff;

import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StaffModeManager {

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private final Set<UUID> staffModePlayers = new HashSet<>();
    private final WinterCore plugin;

    private static final String PUSH_FORWARD    = CC.translate("&6Push Forward");
    private static final String STAFF_LIST      = CC.translate("&eStaff List");
    private static final String RANDOM_TELEPORT = CC.translate("&bRandom Teleport");
    private static final String INSPECT_PLAYER  = CC.translate("&bInspect Player");
    private static final String FREEZE_PLAYER   = CC.translate("&bFreeze Player");
    private static final String VANISH          = CC.translate("&aVanish");
    private static final String UNVANISH        = CC.translate("&7Un-Vanish");

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
            savedInventories.put(uuid, player.getInventory().getContents());
            savedGameModes.put(uuid, player.getGameMode());
        }

        staffModePlayers.add(uuid);
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();

        player.getInventory().setItem(0, createItem(Material.COMPASS, PUSH_FORWARD,
                Collections.singletonList(CC.translate("&7Use this to move forward quickly."))));
        player.getInventory().setItem(1, createItem(Material.SKULL_ITEM, STAFF_LIST,
                Collections.singletonList(CC.translate("&7View the list of online staff members."))));
        player.getInventory().setItem(2, createItem(Material.NETHER_STAR, RANDOM_TELEPORT,
                Collections.singletonList(CC.translate("&7Teleport to a random player."))));
        player.getInventory().setItem(4, createItem(Material.CARPET, CC.translate("&bBetter View"),
                Collections.singletonList(CC.translate("&7Enhance your view for better observation."))));
        player.getInventory().setItem(6, createItem(Material.BOOK, INSPECT_PLAYER,
                Collections.singletonList(CC.translate("&7Inspect a nearby player's inventory."))));
        player.getInventory().setItem(7, createItem(Material.PACKED_ICE, FREEZE_PLAYER,
                Collections.singletonList(CC.translate("&7Freeze a nearby player."))));

        updateVanishItem(player);
        refreshNameTag(player, true);
        player.sendMessage(CC.translate("&aStaff mode enabled."));
    }

    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        player.getInventory().clear();

        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.remove(uuid));
        }

        player.setGameMode(savedGameModes.containsKey(uuid)
                ? savedGameModes.remove(uuid)
                : GameMode.SURVIVAL);

        staffModePlayers.remove(uuid);
        refreshNameTag(player, false);
        player.sendMessage(CC.translate("&cStaff mode disabled and inventory restored."));
    }

    public void handleItemUse(Player player, String itemName, Player target, ItemStack item) {
        if (itemName == null) return;

        if (itemName.equals(PUSH_FORWARD)) {
            player.setVelocity(player.getLocation().getDirection().multiply(3.5));

        } else if (itemName.equals(STAFF_LIST)) {
            player.performCommand("stafflist");

        } else if (itemName.equals(RANDOM_TELEPORT)) {
            teleportToRandomPlayer(player);

        } else if (itemName.equals(INSPECT_PLAYER)) {
            if (target != null) player.performCommand("invsee " + target.getName());
            else player.sendMessage(CC.translate("&cNo player nearby to inspect."));

        } else if (itemName.equals(FREEZE_PLAYER)) {
            if (target != null) player.performCommand("freeze " + target.getName());
            else player.sendMessage(CC.translate("&cNo player nearby to freeze."));

        } else if (itemName.equals(VANISH) || itemName.equals(UNVANISH)) {
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
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasStaffPermission(p)) nonStaff.add(p);
        }

        if (nonStaff.isEmpty()) {
            player.sendMessage(CC.translate("&cNo non-staff players online."));
            return;
        }

        Player target = nonStaff.get(new Random().nextInt(nonStaff.size()));
        player.teleport(target);
        player.sendMessage(CC.translate("&aTeleported to " + target.getName() + "."));
    }

    private void updateVanishItem(Player player) {
        boolean vanished = VanishCommand.isVanished(player);
        player.getInventory().setItem(8, createItem(
                Material.INK_SACK,
                vanished ? UNVANISH : VANISH,
                (short) (vanished ? 8 : 10),
                Collections.singletonList(CC.translate("&7Toggle vanish mode."))
        ));
    }

    public Player getTargetPlayer(Player player) {
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || !player.canSee(other)) continue;
            double dist = player.getLocation().distance(other.getLocation());
            if (dist <= 5 && dist < closestDist) {
                closestDist = dist;
                closest = other;
            }
        }
        return closest;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, (short) 0, lore);
    }

    private ItemStack createItem(Material material, String name, short data, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}