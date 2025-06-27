package net.curxxed.dev.wintercore.disguise.commands;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;
import net.curxxed.dev.wintercore.disguise.DisguiseGUI;

public class DisguiseCommand extends BaseCommand {
    private final DisguiseHandler disguiseHandler;

    public DisguiseCommand(DisguiseHandler disguiseHandler) {
        this.disguiseHandler = disguiseHandler;
    }

    @Command(
            name = "disguise",
            description = "Disguise as another player.",
            usage = "/disguise <name>",
            inGameOnly = true,
            permission = "wintercore.disguise"
    )
    public void onCommand(CommandArgs commandArgs) {
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
        String name = args[0];
        DisguiseGUI.setPendingTarget(player, name); // Store pending disguise target
        disguiseHandler.openRankSelectionGUI(player, name);
    }
}
