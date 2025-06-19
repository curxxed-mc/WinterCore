package net.curxxed.dev.icore.disguise.commands;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.disguise.DisguiseHandler;
import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.entity.Player;
import net.curxxed.dev.icore.disguise.DisguiseGUI;

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
            permission = "icore.disguise"
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (iCore.INSTANCE.getDisguiseDataMap().containsKey(player.getUniqueId())) {
            player.sendMessage(CC.translate("&cYou are already disguised! Use /undisguise first."));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(CC.translate("&cUsage: /disguise <name>"));
            return;
        }
        String name = args[0];
        if (name.length() < 3 || name.length() > 16 || !name.matches("^[A-Za-z0-9_]+$")) {
            player.sendMessage(CC.translate("&cName must be 3-16 characters and only contain letters, numbers, or underscores."));
            return;
        }
        DisguiseGUI.setPendingTarget(player, name); // Store pending disguise target
        disguiseHandler.openRankSelectionGUI(player, name);
    }
}
