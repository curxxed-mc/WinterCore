package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Arrays;
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
            sendUsage(commandArgs.getSender());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getName() == null) {
            send(commandArgs.getSender(), "general.player-not-found", "&cPlayer not found.");
            return;
        }

        plugin.getDatabaseManager().getIdentityService().getAlts(target.getUniqueId(), alts -> runSync(() -> {
            if (alts.isEmpty()) {
                send(commandArgs.getSender(), "alts.none", "&a{target} has no detected alts.",
                        "{target}", target.getName());
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
                            String joined = formatted.stream().collect(Collectors.joining(msg("alts.separator", "&f, ")));
                            int count = altList.size();
                            sendList(commandArgs.getSender(), "alts.list", Arrays.asList(
                                    "&3------[ &bAlts of &e{target} &b(&d{count}&b) &3]------",
                                    "{alts}",
                                    "&8[&aOnline&8, &cMuted&8, &4Banned&8, &7Offline&8]",
                                    "&3----------------------------------------"
                            ), "{target}", target.getName(),
                                    "{count}", String.valueOf(count),
                                    "{alts}", joined);
                        }
                    }))
            );
        }
    }
}
