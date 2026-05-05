package net.curxxed.dev.wintercore.commands.troll;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@CommandInfo(
        name = "troll",
        description = "Troll a player with various effects.",
        usage = "/troll <player> <demo|win|boatspam|daynight>",
        inGameOnly = true,
        permission = {"wintercore.troll"}
)
public class TrollCommand extends BaseCommand {

    private final Map<UUID, BukkitRunnable> dayNightTasks = new HashMap<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList("demo", "win", "boatspam", "daynight");

    public TrollCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (commandArgs.length() < 2) {
            player.sendMessage(CC.RED + "Usage: /troll <player> <" + String.join("|", SUBCOMMANDS) + ">");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(CC.RED + "Player not found or offline.");
            return;
        }

        String sub = commandArgs.getArgs()[1].toLowerCase();
        switch (sub) {
            case "demo":
                sendGameStateChange(target, 5, 0.0f);
                break;
            case "win":
                sendGameStateChange(target, 4, 0.0f);
                break;
            case "boatspam":
                spawnFakeBoats(target);
                break;
            case "daynight":
                toggleDayNightLoop(player, target);
                return;
            default:
                player.sendMessage(CC.RED + "Unknown troll type. Use: " + String.join(", ", SUBCOMMANDS));
                return;
        }

        player.sendMessage(CC.GREEN + "Trolled " + target.getName() + " with " + sub + "!");
    }

    private void sendGameStateChange(Player target, int reason, float value) {
        try {
            Class<?> packetClass = Utilities.getNMSClass("PacketPlayOutGameStateChange");
            Constructor<?> ctor = Utilities.getConstructorByParamCount(packetClass, 2);
            Utilities.sendPacket(target, ctor.newInstance(reason, value));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send game state packet to " + target.getName() + ": " + e.getMessage());
        }
    }

    private void spawnFakeBoats(Player target) {
        Location base = target.getLocation().add(0, 2, 0);

        for (int i = 0; i < 10; i++) {
            Location loc = base.clone().add(0, i * 0.25, 0);
            try {
                Class<?> packetClass = Utilities.getNMSClass("PacketPlayOutSpawnEntity");
                Object packet = packetClass.newInstance();
                int entityId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

                if (Utilities.IS_1_7) {
                    Utilities.setField(packet, "a", entityId);
                    Utilities.setField(packet, "b", (int) (loc.getX() * 32));
                    Utilities.setField(packet, "c", (int) (loc.getY() * 32));
                    Utilities.setField(packet, "d", (int) (loc.getZ() * 32));
                    Utilities.setField(packet, "e", 0);
                    Utilities.setField(packet, "f", 0);
                    Utilities.setField(packet, "g", 0);
                    Utilities.setField(packet, "h", 0);
                    Utilities.setField(packet, "i", 0);
                    Utilities.setField(packet, "j", 1);
                    Utilities.setField(packet, "k", 0);
                } else {
                    Utilities.setField(packet, "a", entityId);
                    Utilities.setField(packet, "b", UUID.randomUUID());
                    Utilities.setField(packet, "c", (int) (loc.getX() * 32));
                    Utilities.setField(packet, "d", (int) (loc.getY() * 32));
                    Utilities.setField(packet, "e", (int) (loc.getZ() * 32));
                    Utilities.setField(packet, "f", 0);
                    Utilities.setField(packet, "g", 0);
                    Utilities.setField(packet, "h", 0);
                    Utilities.setField(packet, "i", 0);
                    Utilities.setField(packet, "j", 0);
                    Utilities.setField(packet, "k", 1);
                    Utilities.setField(packet, "l", 0);
                }

                Utilities.sendPacket(target, packet);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send fake boat packet: " + e.getMessage());
            }
        }
    }

    private void toggleDayNightLoop(Player sender, Player target) {
        UUID uuid = target.getUniqueId();

        if (dayNightTasks.containsKey(uuid)) {
            dayNightTasks.get(uuid).cancel();
            dayNightTasks.remove(uuid);
            sender.sendMessage(CC.GREEN + "Stopped day/night loop for " + target.getName() + ".");
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

        sender.sendMessage(CC.GREEN + "Started day/night loop for " + target.getName() + ".");
        target.sendMessage(CC.RED + "Something feels off about time...");
    }

    private void sendTimeUpdate(Player target, long timeOfDay) {
        try {
            Class<?> packetClass = Utilities.getNMSClass("PacketPlayOutUpdateTime");
            Object packet = packetClass
                    .getConstructor(long.class, long.class)
                    .newInstance(target.getWorld().getFullTime(), timeOfDay);
            Utilities.sendPacket(target, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send time packet to " + target.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            return completeOnlinePlayers(args);
        }

        if (args.length() == 2) {
            return completeCurrentArg(args, SUBCOMMANDS);
        }

        return Collections.emptyList();
    }
}





