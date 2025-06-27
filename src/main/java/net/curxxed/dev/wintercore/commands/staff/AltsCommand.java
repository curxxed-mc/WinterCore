package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AltsCommand extends BaseCommand {
    private final WinterCore plugin;

    public AltsCommand(WinterCore plugin) {
        this.plugin = plugin;
    }

    @Command(
        name = "alts",
        permission = "WinterCore.alts",
        description = "View all accounts that have joined from the same IP(s) as a player.",
        usage = "/alts <player>",
        inGameOnly = false
    )
    @Override
    public void onCommand(CommandArgs commandArgs) {
        if (!commandArgs.getSender().hasPermission("WinterCore.alts")) {
            commandArgs.getSender().sendMessage(CC.translate("&cYou do not have permission to use this command."));
            return;
        }
        String[] args = commandArgs.getArgs();
        if (args.length != 1) {
            commandArgs.getSender().sendMessage(CC.translate("&cUsage: /alts <player>"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getName() == null) {
            commandArgs.getSender().sendMessage(CC.translate("&cPlayer not found."));
            return;
        }
        plugin.getDatabaseManager().getAlts(target.getUniqueId(), alts -> {
            if (alts.isEmpty()) {
                commandArgs.getSender().sendMessage(CC.translate("&a" + target.getName() + " has no detected alts."));
                return;
            }
            List<UUID> altList = new ArrayList<>(alts);
            AtomicInteger completed = new AtomicInteger(0);
            List<String> formatted = Collections.synchronizedList(new ArrayList<>());
            for (UUID altUUID : altList) {
                OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUUID);
                String name = altPlayer.getName() != null ? altPlayer.getName() : altUUID.toString();
                boolean isOnline = altPlayer.isOnline();
                plugin.getDatabaseManager().isPlayerMuted(altUUID, isMuted -> {
                    plugin.getDatabaseManager().isPlayerBanned(altUUID, isBanned -> {
                        String color;
                        if (isBanned) {
                            color = CC.translate("&4"); // DARK_RED
                        } else if (isMuted) {
                            color = CC.translate("&c"); // RED
                        } else if (isOnline) {
                            color = CC.translate("&a"); // GREEN
                        } else {
                            color = CC.translate("&7"); // GRAY
                        }
                        formatted.add(color + name);
                        if (completed.incrementAndGet() == altList.size()) {
                            String joined = formatted.stream().collect(Collectors.joining(CC.translate("&f, ")));
                            int count = altList.size();
                            commandArgs.getSender().sendMessage(CC.translate("&3------[ &bAlts of &e" + target.getName() + " &b(&d" + count + "&b) &3]------"));
                            commandArgs.getSender().sendMessage(joined);
                            commandArgs.getSender().sendMessage(CC.translate("&8[&aOnline&8, &cMuted&8, &4Banned&8, &7Offline&8]"));
                            commandArgs.getSender().sendMessage(CC.translate("&3----------------------------------------"));
                        }
                    });
                });
            }
        });
    }
}
