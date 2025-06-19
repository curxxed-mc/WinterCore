package net.curxxed.dev.icore.commands.troll;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.listeners.ReachListener;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TrollCommand extends BaseCommand {

    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final iCore plugin;
    private final ReachListener reachListener;
    private final Map<UUID, BukkitRunnable> weatherTasks = new HashMap<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList("demo", "win", "boatspam", "reach", "daynight");

    public TrollCommand(iCore plugin, ReachListener reachListener) {
        this.plugin = plugin;
        this.reachListener = reachListener;
    }

    @Command(
            name = "troll",
            permission = "iCore.troll",
            description = "troll a player with various effects.",
            usage = "/troll <player> <demo|win|boatspam|reach|daynight>",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        if (commandArgs.length() != 2) {
            commandArgs.getSender().sendMessage(ChatColor.RED + "Usage: /troll <player> <" + String.join("|", SUBCOMMANDS) + ">");
            return;
        }
        Player player = commandArgs.getPlayer();
        Player target = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        String type = commandArgs.getArgs(1).toLowerCase();
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
                if (reachListener != null) {
                    reachListener.applyReach(player, target);
                }
                break;
            case "daynight":
                toggleDayNightLoop(target);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown troll type.");
                return;
        }

        player.sendMessage(ChatColor.GREEN + "Trolled " + target.getName() + " with " + type + "!");
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

            packet.getIntegers().write(0, entityId);
            packet.getUUIDs().write(0, uuid);
            packet.getEntityTypeModifier().write(0, EntityType.BOAT);
            packet.getDoubles().write(0, dropLoc.getX());
            packet.getDoubles().write(1, dropLoc.getY());
            packet.getDoubles().write(2, dropLoc.getZ());
            packet.getIntegers().write(6, 0);
            packet.getIntegers().write(7, 0);
            packet.getIntegers().write(8, 0);

            try {
                protocolManager.sendServerPacket(target, packet);

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

        if (weatherTasks.containsKey(targetUUID)) {
            weatherTasks.get(targetUUID).cancel();
            weatherTasks.remove(targetUUID);
            target.sendMessage(ChatColor.GREEN + "Day-night cycle loop stopped.");
            return;
        }

        BukkitRunnable dayNightLoopTask = new BukkitRunnable() {
            private long time = 0;

            @Override
            public void run() {
                time = (time + 1000) % 24000;
                sendGameStateChange(target, 7, time);
            }
        };

        dayNightLoopTask.runTaskTimer(plugin, 0L, 2L);
        weatherTasks.put(targetUUID, dayNightLoopTask);

        target.sendMessage(ChatColor.GREEN + "Day-night cycle loop started at 50x speed.");
    }
}