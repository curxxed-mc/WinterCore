package net.curxxed.dev.wintercore.disguise.commands;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "disguise",
        description = "Disguise as another player.",
        usage = "/disguise <name>",
        inGameOnly = true,
        permission = "wintercore.disguise"
)
public class DisguiseCommand extends BaseCommand {

    private final DisguiseHandler disguiseHandler;

    public DisguiseCommand(DisguiseHandler disguiseHandler, WinterCore plugin) {
        super(plugin);
        this.disguiseHandler = disguiseHandler;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (WinterCore.getInstance().getDisguiseDataMap().containsKey(player.getUniqueId())) {
            send(player, "disguise.already-disguised", "&cYou are already disguised! Use /undisguise first.");
            return;
        }

        if (args.length != 1) {
            sendUsage(player);
            return;
        }

        disguiseHandler.openDisguiseMenu(player, args[0]);
    }
}
