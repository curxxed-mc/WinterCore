package curxxed.dev.icore.Commands.Staff;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import curxxed.dev.icore.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StaffModeCommand implements CommandExecutor, Listener {

    private final Main plugin;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, GameMode> savedGameModes = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> staffModePlayers = new HashSet<>();
    private static final long COOLDOWN_TIME = 950;

    private static final String PUSH_FORWARD = ChatColor.GOLD + "Push Forward";
    private static final String STAFF_LIST = ChatColor.YELLOW + "Staff List";
    private static final String RANDOM_TELEPORT = ChatColor.AQUA + "Random Teleport";
    private static final String INSPECT_PLAYER = ChatColor.AQUA + "Inspect Player";
    private static final String FREEZE_PLAYER = ChatColor.AQUA + "Freeze Player";
    private static final String VANISH = ChatColor.GREEN + "Vanish";
    private static final String UNVANISH = ChatColor.GRAY + "Un-Vanish";

    public StaffModeCommand(Main plugin) {
        this.plugin = plugin;
        setupProtocolLib();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!hasStaffPermission(player)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (isInStaffMode(player)) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }

        return true;
    }

    private boolean hasStaffPermission(Player player) {
        return player.hasPermission("iCore.staff") || player.hasPermission("iCore.admin") || player.hasPermission("iCore.manager") || player.isOp();
    }

    public boolean isInStaffMode(Player player) {
        return staffModePlayers.contains(player.getUniqueId());
    }

    private void enableStaffMode(Player player) {
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

        boolean vanished = VanishCommand.vanishedPlayers.contains(player.getUniqueId());
        player.getInventory().setItem(8, createItem(Material.INK_SACK, vanished ? VANISH : UNVANISH, (short) (vanished ? 10 : 8),
                Collections.singletonList(ChatColor.GRAY + "Toggle vanish mode.")));

        player.sendMessage(ChatColor.GREEN + "Staff mode enabled.");
    }

    private void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        player.getInventory().clear();

        if (savedInventories.containsKey(uuid)) {
            player.getInventory().setContents(savedInventories.get(uuid));
            savedInventories.remove(uuid);
        }

        if (savedGameModes.containsKey(uuid)) {
            player.setGameMode(savedGameModes.get(uuid));
            savedGameModes.remove(uuid);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        staffModePlayers.remove(uuid);
        player.sendMessage(ChatColor.RED + "Staff mode disabled and inventory restored.");
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        ItemStack item = player.getItemInHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String itemName = item.getItemMeta().getDisplayName();
        Player target = getTargetPlayer(player);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUse = cooldowns.get(uuid);
            if ((now - lastUse) < COOLDOWN_TIME) {
                return;
            }
        }

        cooldowns.put(uuid, now);

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
            toggleVanish(player, item);
        }
    }

    private void teleportToRandomPlayer(Player player) {
        List<Player> nonStaffPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasStaffPermission(p)) {
                nonStaffPlayers.add(p);
            }
        }

        if (nonStaffPlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No non-staff players online.");
            return;
        }

        Player target = nonStaffPlayers.get(new Random().nextInt(nonStaffPlayers.size()));
        player.teleport(target);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + target.getName() + ".");
    }

    private void toggleVanish(Player player, ItemStack dye) {
        player.performCommand("vanish");

        boolean vanished = VanishCommand.vanishedPlayers.contains(player.getUniqueId());
        dye.setDurability((short) (vanished ? 10 : 8));

        ItemMeta meta = dye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(vanished ? VANISH : UNVANISH);
            dye.setItemMeta(meta);
        }

        player.getInventory().setItem(10, dye);
    }

    private Player getTargetPlayer(Player player) {
        double closestDistance = Double.MAX_VALUE;
        Player closest = null;

        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) continue;
            if (!player.canSee(other)) continue;

            double distance = player.getLocation().distance(other.getLocation());
            if (distance <= 5 && distance < closestDistance) {
                closestDistance = distance;
                closest = other;
            }
        }

        return closest;
    }

    private void setupProtocolLib() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
            private void setupProtocolLib() {
                ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
                            EnumWrappers.PlayerInfoAction action = event.getPacket().getPlayerInfoAction().read(0);
                            if (action == EnumWrappers.PlayerInfoAction.ADD_PLAYER || action == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME) {
                                List<PlayerInfoData> dataList = event.getPacket().getPlayerInfoDataLists().read(0);
                                List<PlayerInfoData> modifiedDataList = new ArrayList<>();

                                for (PlayerInfoData data : dataList) {
                                    Player player = Bukkit.getPlayer(data.getProfile().getUUID());
                                    if (player != null && staffModePlayers.contains(player.getUniqueId())) {
                                        WrappedGameProfile profile = data.getProfile();
                                        String newName = "*" + profile.getName();
                                        WrappedGameProfile modifiedProfile = new WrappedGameProfile(profile.getUUID(), newName);

                                        // Copy skin properties from the original profile
                                        for (Map.Entry<String, Collection<WrappedSignedProperty>> entry : profile.getProperties().asMap().entrySet()) {
                                            for (WrappedSignedProperty property : entry.getValue()) {
                                                modifiedProfile.getProperties().put(entry.getKey(), property);
                                            }
                                        }

                                        // Create a new PlayerInfoData with the modified profile
                                        PlayerInfoData modifiedData = new PlayerInfoData(
                                                modifiedProfile,
                                                data.getLatency(),
                                                data.getGameMode(),
                                                data.getDisplayName()
                                        );
                                        modifiedDataList.add(modifiedData);
                                    } else {
                                        modifiedDataList.add(data);
                                    }
                                }

                                // Replace the original data list with the modified one
                                event.getPacket().getPlayerInfoDataLists().write(0, modifiedDataList);
                            }
                        }
                    }
                });
            }
        });
    }
}