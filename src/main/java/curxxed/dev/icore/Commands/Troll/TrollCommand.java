package curxxed.dev.icore.Commands.Troll;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import curxxed.dev.icore.iCore;
import curxxed.dev.icore.listeners.ReachListener;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class TrollCommand implements CommandExecutor, TabCompleter {

    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final iCore plugin;
    private final ReachListener reachListener;
    private final Map<UUID, BukkitRunnable> weatherTasks = new HashMap<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList("demo", "win", "boatspam", "reach", "daynight");

    public TrollCommand(iCore plugin, ReachListener reachListener) {
        this.plugin = plugin;
        this.reachListener = reachListener;
        plugin.getCommand("troll").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /troll <player> <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return true;
        }

        String type = args[1].toLowerCase();
        switch (type) {
            case "demo":
                sendGameStateChange(target, 5, 0.0f);
                break;
            case "win":
                sendGameStateChange(target, 4, 0.0f);
                break;
            case "boatspam":
                spawnBoatStack(target);
                break;
            case "reach":
                reachListener.applyReach(player, target);
                break;
            case "daynight":
                toggleDayNightLoop(target);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown troll type.");
                return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Trolled " + target.getName() + " with " + type + "!");
        return true;
    }

    private void sendGameStateChange(Player target, int reason, float value) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
            packet.getIntegers().write(0, reason);
            packet.getFloat().write(0, value);
            protocolManager.sendServerPacket(target, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnBoatStack(Player target) {
        Location loc = target.getLocation().add(0, 2, 0);
        int amount = 10;

        for (int i = 0; i < amount; i++) {
            Location dropLoc = loc.clone().add(0, i * 0.25, 0);

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            int entityId = (int) (Math.random() * Integer.MAX_VALUE);
            UUID uuid = UUID.randomUUID();

            packet.getIntegers().write(0, entityId); // Entity ID
            packet.getUUIDs().write(0, uuid);
            packet.getEntityTypeModifier().write(0, EntityType.BOAT); // Corrected to EntityType
            packet.getDoubles().write(0, dropLoc.getX());
            packet.getDoubles().write(1, dropLoc.getY());
            packet.getDoubles().write(2, dropLoc.getZ());
            packet.getIntegers().write(6, 0); // velocity X
            packet.getIntegers().write(7, 0); // velocity Y
            packet.getIntegers().write(8, 0); // velocity Z

            try {
                protocolManager.sendServerPacket(target, packet);

                // Also send item metadata (boat stack)
                PacketContainer itemPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                itemPacket.getIntegers().write(0, entityId);

                WrappedDataWatcher watcher = new WrappedDataWatcher();
                WrappedDataWatcher.Serializer itemSerializer = WrappedDataWatcher.Registry.getItemStackSerializer(false);
                watcher.setObject(10, itemSerializer, new ItemStack(Material.BOAT));

                itemPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                protocolManager.sendServerPacket(target, itemPacket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void toggleDayNightLoop(Player target) {
        UUID targetUUID = target.getUniqueId();

        // If a day-night loop is already running for the player, cancel it
        if (weatherTasks.containsKey(targetUUID)) {
            weatherTasks.get(targetUUID).cancel();
            weatherTasks.remove(targetUUID);
            target.sendMessage(ChatColor.GREEN + "Day-night cycle loop stopped.");
            return;
        }

        // Create a new day-night loop task
        BukkitRunnable dayNightLoopTask = new BukkitRunnable() {
            private long time = 0; // Start at 0 ticks

            @Override
            public void run() {
                // Increment time by 1000 ticks (50x speed)
                time = (time + 1000) % 24000;

                // Send the updated time to the player
                sendGameStateChange(target, 7, time); // 7 = Change time
            }
        };

        // Schedule the task to run every 2 ticks (0.1 seconds)
        dayNightLoopTask.runTaskTimer(plugin, 0L, 2L);
        weatherTasks.put(targetUUID, dayNightLoopTask);

        target.sendMessage(ChatColor.GREEN + "Day-night cycle loop started at 50x speed.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()); // Fixed to use Collectors.toList()
        }
        if (args.length == 2) {
            return SUBCOMMANDS;
        }
        return Collections.emptyList();
    }
}