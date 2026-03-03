package net.curxxed.dev.wintercore.staff;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.curxxed.dev.wintercore.commands.staff.VanishCommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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

    private static final String PUSH_FORWARD    = ChatColor.GOLD + "Push Forward";
    private static final String STAFF_LIST      = ChatColor.YELLOW + "Staff List";
    private static final String RANDOM_TELEPORT = ChatColor.AQUA + "Random Teleport";
    private static final String INSPECT_PLAYER  = ChatColor.AQUA + "Inspect Player";
    private static final String FREEZE_PLAYER   = ChatColor.AQUA + "Freeze Player";
    private static final String VANISH          = ChatColor.GREEN + "Vanish";
    private static final String UNVANISH        = ChatColor.GRAY + "Un-Vanish";

    public StaffModeManager(WinterCore plugin) {
        this.plugin = plugin;
    }

    public boolean hasStaffPermission(Player player) {
        return player.hasPermission("wintercore.staff") || player.hasPermission("wintercore.admin")
                || player.hasPermission("wintercore.manager") || player.isOp();
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
                Collections.singletonList(ChatColor.GRAY + "Use this to move forward quickly.")));
        player.getInventory().setItem(1, createItem(Material.SKULL_ITEM, STAFF_LIST,
                Collections.singletonList(ChatColor.GRAY + "View the list of online staff members.")));
        player.getInventory().setItem(2, createItem(Material.NETHER_STAR, RANDOM_TELEPORT,
                Collections.singletonList(ChatColor.GRAY + "Teleport to a random player.")));
        player.getInventory().setItem(4, createItem(Material.CARPET, ChatColor.AQUA + "Better View",
                Collections.singletonList(ChatColor.GRAY + "Enhance your view for better observation.")));
        player.getInventory().setItem(6, createItem(Material.BOOK, INSPECT_PLAYER,
                Collections.singletonList(ChatColor.GRAY + "Inspect a nearby player's inventory.")));
        player.getInventory().setItem(7, createItem(Material.PACKED_ICE, FREEZE_PLAYER,
                Collections.singletonList(ChatColor.GRAY + "Freeze a nearby player.")));

        updateVanishItem(player);
        player.sendMessage(ChatColor.GREEN + "Staff mode enabled.");
    }

    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        player.getInventory().clear();

        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.remove(uuid));
        }

        if (savedGameModes.containsKey(uuid)) {
            player.setGameMode(savedGameModes.remove(uuid));
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        staffModePlayers.remove(uuid);
        player.sendMessage(ChatColor.RED + "Staff mode disabled and inventory restored.");
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
            if (target != null) {
                player.performCommand("invsee " + target.getName());
            } else {
                player.sendMessage(ChatColor.RED + "No player nearby to inspect.");
            }

        } else if (itemName.equals(FREEZE_PLAYER)) {
            if (target != null) {
                player.performCommand("freeze " + target.getName());
            } else {
                player.sendMessage(ChatColor.RED + "No player nearby to freeze.");
            }

        } else if (itemName.equals(VANISH) || itemName.equals(UNVANISH)) {
            VanishCommand.toggleVanish(player, plugin, vanished -> {
                updateVanishItem(player); // refresh dye after vanish status toggle
            });
        }
    }


    private void teleportToRandomPlayer(Player player) {
        List<Player> nonStaff = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasStaffPermission(p)) {
                nonStaff.add(p);
            }
        }

        if (nonStaff.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No non-staff players online.");
            return;
        }

        Player target = nonStaff.get(new Random().nextInt(nonStaff.size()));
        player.teleport(target);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
    }

    private void updateVanishItem(Player player) {
        boolean vanished = VanishCommand.isVanished(player);
        String name = vanished ? UNVANISH : VANISH;
        short color = (short) (vanished ? 8 : 10);
        ItemStack dye = createItem(Material.INK_SACK, name, color,
                Collections.singletonList(ChatColor.GRAY + "Toggle vanish mode."));
        player.getInventory().setItem(8, dye);
    }

    public Player getTargetPlayer(Player player) {
        double closestDistance = Double.MAX_VALUE;
        Player closest = null;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || !player.canSee(other)) continue;
            double dist = player.getLocation().distance(other.getLocation());
            if (dist <= 5 && dist < closestDistance) {
                closestDistance = dist;
                closest = other;
            }
        }
        return closest;
    }

    public void modifyPlayerInfoPacket(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        int version = Integer.parseInt(ProtocolLibrary.getProtocolManager().getMinecraftVersion().getVersion());
        if (version <= 1.21) {
            EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);
            if (action != EnumWrappers.PlayerInfoAction.ADD_PLAYER && action != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME)
                return;

            StructureModifier<List<PlayerInfoData>> dataModifier = packet.getPlayerInfoDataLists();
            if (dataModifier.size() == 0) return;

            List<PlayerInfoData> dataList = dataModifier.read(0);
            if (dataList == null || dataList.isEmpty()) return;

            List<PlayerInfoData> modifiedList = new ArrayList<>();

            for (PlayerInfoData data : dataList) {
                Player p = Bukkit.getPlayer(data.getProfile().getUUID());
                if (p != null && staffModePlayers.contains(p.getUniqueId())) {
                    WrappedGameProfile prof = data.getProfile();
                    WrappedGameProfile modProf = new WrappedGameProfile(prof.getUUID(), "*" + prof.getName());
                    prof.getProperties().asMap().forEach((k, v) ->
                            v.forEach(prop -> modProf.getProperties().put(k, prop)));
                    modifiedList.add(new PlayerInfoData(modProf, data.getLatency(), data.getGameMode(), data.getDisplayName()));
                } else {
                    modifiedList.add(data);
                }
            }
            dataModifier.write(0, modifiedList);
        }

        // Post-1.19 logic (ClientboundPlayerInfoUpdatePacket)
        else {
            try {
                // Minecraft 1.19+ rewrites this as permissions record-based structure.
                List<?> entries = packet.getSpecificModifier(List.class).read(0);
                if (entries == null || entries.isEmpty()) return;

                // Loop through each entry and use reflection to get values
                List<Object> newEntries = new ArrayList<>();
                for (Object entry : entries) {
                    UUID uuid = (UUID) entry.getClass().getMethod("playerId").invoke(entry);
                    Player p = Bukkit.getPlayer(uuid);

                    if (p != null && staffModePlayers.contains(p.getUniqueId())) {
                        // Reflectively change the profile name (must create permissions new GameProfile or modify raw entry)
                        // NOTE: This step will vary depending on the exact server implementation, and you may need to reflectively rebuild the entry object.

                        // Skip editing for now; just remove them from tablist for demo purposes
                        continue; // Or optionally: don't add to `newEntries` at all
                    }

                    newEntries.add(entry);
                }

                // Write the modified entries back into the packet
                packet.getSpecificModifier(List.class).write(0, newEntries);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, (short) 0, lore);
    }

    private ItemStack createItem(Material material, String name, short data, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
