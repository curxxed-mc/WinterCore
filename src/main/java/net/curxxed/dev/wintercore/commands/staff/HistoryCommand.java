package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.menus.HistoryMenu;
import net.curxxed.dev.wintercore.config.MenuConfig;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "history",
        description = "View a player's punishment and grant history.",
        usage = "/history <player>",
        async = true,
        permission = {"wintercore.history"}
)
public class HistoryCommand extends BaseCommand {

    private final MenuConfig menuConfig;

    public HistoryCommand(WinterCore plugin, MenuConfig menuConfig) {
        super(plugin);
        this.menuConfig = menuConfig;
    }

    @Override
    public void execute(CommandArguments args) {
        runSync(() -> executeOnMainThread(args));
    }

    private void executeOnMainThread(CommandArguments args) {
        if (args.length() < 1) {
            sendUsage(args.getSender());
            return;
        }

        if (!args.isPlayer()) {
            send(args.getSender(), "history.player-only",
                    "&cOnly players can open the history menu.");
            return;
        }

        Player sender = args.getPlayer();

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args.getArgs()[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            send(sender, "general.player-not-found", "&cPlayer not found.");
            return;
        }

        new HistoryMenu(plugin, menuConfig, target.getName(), target.getUniqueId()).open(sender);
    }
}
