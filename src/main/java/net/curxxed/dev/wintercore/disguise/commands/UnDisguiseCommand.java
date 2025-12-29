package net.curxxed.dev.wintercore.disguise.commands;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "undisguise",
        description = "Remove your disguise.",
        usage = "/undisguise",
        permission = "wintercore.disguise",
        inGameOnly = true
)
public class UnDisguiseCommand extends BaseCommand {
    private final DisguiseHandler disguiseHandler;

    public UnDisguiseCommand(DisguiseHandler disguiseHandler, WinterCore plugin) {
        super(plugin);
        this.disguiseHandler = disguiseHandler;
    }


    public void execute(CommandArguments commandArgs) {
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