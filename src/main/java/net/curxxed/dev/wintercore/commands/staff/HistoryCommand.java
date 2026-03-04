package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.menus.HistoryMenu;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@CommandInfo(
        name = "history",
        permission = "wintercore.history",
        description = "View a player's punishment and grant history.",
        usage = "/history <player>",
        async = true
)
public class HistoryCommand extends BaseCommand implements Listener {

    private final HistoryMenu historyMenu;

    public HistoryCommand(WinterCore plugin, HistoryMenu historyMenu) {
        super(plugin);
        this.historyMenu = historyMenu;
    }

    @Override
    public void execute(CommandArguments args) {
        if (args.length() < 1) {
            args.getSender().sendMessage(CC.translate("&cUsage: /history <player>"));
            return;
        }

        if (!args.isPlayer()) {
            args.getSender().sendMessage(CC.translate("&cOnly players can open the history menu."));
            return;
        }

        Player sender = args.getPlayer();

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.getArgs()[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(CC.translate("&cPlayer not found."));
            return;
        }

        historyMenu.open(sender, target.getName(), target.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        historyMenu.clearContext(event.getPlayer().getUniqueId());
    }
}