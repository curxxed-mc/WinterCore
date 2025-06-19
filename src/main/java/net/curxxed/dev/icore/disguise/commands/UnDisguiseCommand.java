package net.curxxed.dev.icore.disguise.commands;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.icore.disguise.DisguiseHandler;
import net.curxxed.dev.icore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.icore.utils.CC;
import org.bukkit.entity.Player;

public class UnDisguiseCommand extends BaseCommand {
    private final DisguiseHandler disguiseHandler;

    public UnDisguiseCommand(DisguiseHandler disguiseHandler) {
        this.disguiseHandler = disguiseHandler;
    }

    @Command(
            name = "undisguise",
            description = "Remove your disguise.",
            usage = "/undisguise",
            permission = "icore.disguise",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        try {
            DisguiseCallback result = disguiseHandler.unDisguise(player, true);
            if (result == DisguiseCallback.SUCCESS) {
                player.sendMessage(CC.translate("&aYou are no longer disguised."));
            } else if (result == DisguiseCallback.NOT_DISGUISED) {
                player.sendMessage(CC.translate("&cYou are not disguised."));
            } else {
                player.sendMessage(CC.translate("&cAn error occurred while undisguising."));
            }
        } catch (Exception e) {
            player.sendMessage(CC.translate("&cAn error occurred while undisguising."));
            e.printStackTrace();
        }
    }
}