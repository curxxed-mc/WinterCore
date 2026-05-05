package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@CommandInfo(
        name = "alts",
        description = "View all accounts that have joined from the same IP(s) as a player.",
        usage = "/alts <player>",
        inGameOnly = false,
        async = true,
        permission = {"wintercore.alts", "WinterCore.alts"}
)
public class AltsCommand extends BaseCommand {
    private final WinterCore plugin;

    public AltsCommand(WinterCore plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        runSync(() -> executeOnMainThread(commandArgs));
    }

    private void executeOnMainThread(CommandArguments commandArgs) {
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

        plugin.getDatabaseManager().getIdentityService().getAlts(target.getUniqueId(), alts -> runSync(() -> {
            if (alts.isEmpty()) {
                commandArgs.getSender().sendMessage(CC.translate("&a" + target.getName() + " has no detected alts."));
                return;
            }

            sendAltList(commandArgs, target, alts);
        }));
    }

    private void sendAltList(CommandArguments commandArgs, OfflinePlayer target, Set<UUID> alts) {
        List<UUID> altList = new ArrayList<>(alts);
        AtomicInteger completed = new AtomicInteger(0);
        List<String> formatted = Collections.synchronizedList(new ArrayList<>());

        for (UUID altUUID : altList) {
            OfflinePlayer altPlayer = Bukkit.getOfflinePlayer(altUUID);
            String name = altPlayer.getName() != null ? altPlayer.getName() : altUUID.toString();
            boolean isOnline = altPlayer.isOnline();

            plugin.getDatabaseManager().getModerationService().isPlayerMuted(altUUID, isMuted ->
                    plugin.getDatabaseManager().getModerationService().isPlayerBanned(altUUID, isBanned -> runSync(() -> {
                        String color;
                        if (isBanned) {
                            color = CC.translate("&4");
                        } else if (isMuted) {
                            color = CC.translate("&c");
                        } else if (isOnline) {
                            color = CC.translate("&a");
                        } else {
                            color = CC.translate("&7");
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
                    }))
            );
        }
    }
}
