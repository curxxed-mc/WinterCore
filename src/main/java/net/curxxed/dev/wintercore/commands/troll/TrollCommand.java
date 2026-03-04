package net.curxxed.dev.wintercore.commands.troll;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@CommandInfo(
        name = "troll",
        permission = "wintercore.troll",
        description = "Troll a player with various effects.",
        usage = "/troll <player> <demo|win|boatspam|daynight>",
        inGameOnly = true
)
public class TrollCommand extends BaseCommand {

    private final WinterCore plugin;
    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    private final Map<UUID, BukkitRunnable> dayNightTasks = new HashMap<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList("demo", "win", "boatspam", "daynight");

    public TrollCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (commandArgs.length() < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /troll <player> <" + String.join("|", SUBCOMMANDS) + ">");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        switch (commandArgs.getArgs()[1].toLowerCase()) {
            case "demo":
                sendGameStateChange(target, 5, 0.0f);
                break;
            case "win":
                sendGameStateChange(target, 4, 0.0f);
                break;
            case "boatspam":
                spawnBoatStack(target);
                break;
            case "daynight":
                toggleDayNightLoop(player, target);
                return;
            default:
                player.sendMessage(ChatColor.RED + "Unknown troll type. Use: " + String.join(", ", SUBCOMMANDS));
                return;
        }

        player.sendMessage(ChatColor.GREEN + "Trolled " + target.getName() + " with " + commandArgs.getArgs()[1].toLowerCase() + "!");
    }

    private void sendGameStateChange(Player target, int reason, float value) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
            packet.getIntegers().write(0, reason);
            packet.getFloat().write(0, value);
            protocolManager.sendServerPacket(target, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send game state packet to " + target.getName() + ": " + e.getMessage());
        }
    }

    private void spawnBoatStack(Player target) {
        Location base = target.getLocation().add(0, 2, 0);

        for (int i = 0; i < 10; i++) {
            Location loc = base.clone().add(0, i * 0.25, 0);

            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                int entityId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

                if (Utilities.IS_1_7) {
                    // 1.7.10 format
                    packet.getIntegers().write(0, entityId);
                    packet.getIntegers().write(1, (int) (loc.getX() * 32));
                    packet.getIntegers().write(2, (int) (loc.getY() * 32));
                    packet.getIntegers().write(3, (int) (loc.getZ() * 32));
                    packet.getIntegers().write(4, 0); // Pitch
                    packet.getIntegers().write(5, 0); // Yaw
                    packet.getIntegers().write(6, 1); // Type ID (1 = Boat)
                    packet.getIntegers().write(7, 0); // Data

                } else {
                    // 1.8.x format
                    packet.getIntegers().write(0, entityId);
                    packet.getUUIDs().write(0, UUID.randomUUID());
                    packet.getEntityTypeModifier().write(0, EntityType.BOAT);
                    packet.getDoubles().write(0, loc.getX());
                    packet.getDoubles().write(1, loc.getY());
                    packet.getDoubles().write(2, loc.getZ());
                }

                protocolManager.sendServerPacket(target, packet);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send fake boat: " + e.getMessage());
            }
        }
    }

    private void toggleDayNightLoop(Player sender, Player target) {
        UUID uuid = target.getUniqueId();

        if (dayNightTasks.containsKey(uuid)) {
            dayNightTasks.get(uuid).cancel();
            dayNightTasks.remove(uuid);
            sender.sendMessage(ChatColor.GREEN + "Stopped day/night loop for " + target.getName() + ".");
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            private long time = 0;

            @Override
            public void run() {
                if (!target.isOnline()) {
                    cancel();
                    dayNightTasks.remove(uuid);
                    return;
                }
                time = (time + 1000) % 24000;
                sendTimeUpdate(target, time);
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        dayNightTasks.put(uuid, task);

        sender.sendMessage(ChatColor.GREEN + "Started day/night loop for " + target.getName() + ".");
        target.sendMessage(ChatColor.RED + "Something feels off about time...");
    }

    private void sendTimeUpdate(Player target, long timeOfDay) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.UPDATE_TIME);
            packet.getLongs().write(0, target.getWorld().getFullTime());
            packet.getLongs().write(1, timeOfDay);
            protocolManager.sendServerPacket(target, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send time packet to " + target.getName() + ": " + e.getMessage());
        }
    }
}