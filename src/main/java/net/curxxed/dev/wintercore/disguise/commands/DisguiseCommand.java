package net.curxxed.dev.wintercore.disguise.commands;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
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

        if (WinterCore.INSTANCE.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            player.sendMessage(CC.translate("&cYou are already disguised! Use /undisguise first."));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(CC.translate("&cUsage: /disguise <name>"));
            return;
        }

        disguiseHandler.openDisguiseMenu(player, args[0]);
    }
}