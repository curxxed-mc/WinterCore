package net.curxxed.dev.wintercore.commands.troll;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
        usage = "/troll <player> <demo|win|boatspam|madeinheaven>",
        inGameOnly = true,
        permission = {"wintercore.troll"}
)
public class TrollCommand extends BaseCommand {

    private final Map<UUID, BukkitTask> dayNightTasks = new HashMap<>();

    private static final List<String> SUBCOMMANDS = Arrays.asList("demo", "win", "boatspam", "madeinheaven");

    public TrollCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (commandArgs.length() < 2) {
            send(player, "troll.usage", "&cUsage: /troll <player> <{types}>",
                    "{types}", String.join("|", SUBCOMMANDS));
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            send(player, "troll.not-online", "&cPlayer not found or offline.");
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
            case "madeinheaven":
                toggleDayNightLoop(player, target);
                return;
            default:
                send(player, "troll.unknown-type", "&cUnknown troll type. Use: {types}",
                        "{types}", String.join(", ", SUBCOMMANDS));
                return;
        }

        send(player, "troll.success", "&aTrolled {target} with {type}!",
                "{target}", target.getName(),
                "{type}", sub);
    }

    private void sendGameStateChange(Player target, int reason, float value) {
        try {
            Class<?> packetClass = Utilities.getNMSClass("PacketPlayOutGameStateChange");
            Object packet;
            if (Utilities.IS_LEGACY) {
                packet = Utilities.getConstructorByParamCount(packetClass, 2).newInstance(reason, value);
            } else {
                Object event = packetClass.getField(reason == 5 ? "DEMO_EVENT" : "WIN_GAME").get(null);
                packet = packetClass.getConstructor(event.getClass(), float.class).newInstance(event, value);
            }
            Utilities.sendPacket(target, packet);
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
                int entityId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
                Object packet;

                if (!Utilities.IS_LEGACY) {
                    Object boatType;
                    try {
                        boatType = Class.forName("net.minecraft.world.entity.EntityTypes").getField("OAK_BOAT").get(null);
                    } catch (ReflectiveOperationException ignored) {
                        boatType = Class.forName("net.minecraft.world.entity.EntityType").getField("BOAT").get(null);
                    }
                    Class<?> vec3 = Class.forName("net.minecraft.world.phys.Vec3");
                    packet = Utilities.getConstructorByParamCount(packetClass, 11).newInstance(
                            entityId, UUID.randomUUID(), loc.getX(), loc.getY(), loc.getZ(),
                            0.0f, 0.0f, boatType, 0, vec3.getField("ZERO").get(null), 0.0D
                    );
                } else {
                    packet = packetClass.newInstance();
                    Utilities.setField(packet, "a", entityId);
                    if (Utilities.getMinecraftMinorVersion() <= 8) {
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
                }

                Utilities.sendPacket(target, packet);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send fake boat packet: " + e.getMessage());
            }
        }
    }

    private void toggleDayNightLoop(Player sender, Player target) {
        UUID uuid = target.getUniqueId();

        BukkitTask activeTask = dayNightTasks.remove(uuid);
        if (activeTask != null) {
            activeTask.cancel();
            target.resetPlayerTime();
            send(sender, "troll.daynight-stopped", "&aStopped day/night loop for {target}.",
                    "{target}", target.getName());
            return;
        }

        Runnable loop = new Runnable() {
            private long time = 0;

            @Override
            public void run() {
                if (!target.isOnline()) {
                    plugin.getTasks().cancel(dayNightTasks.remove(uuid));
                    return;
                }
                time = (time + 1000) % 24000;
                target.setPlayerTime(time, false);
            }
        };

        dayNightTasks.put(uuid, plugin.getTasks().timer(loop, 0L, 2L));

        send(sender, "troll.daynight-started", "&aStarted day/night loop for {target}.",
                "{target}", target.getName());
        send(target, "troll.daynight-target", "&cSomething feels off about time...");
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







