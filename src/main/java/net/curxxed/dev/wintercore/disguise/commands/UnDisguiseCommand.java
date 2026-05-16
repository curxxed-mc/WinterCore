package net.curxxed.dev.wintercore.disguise.commands;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.callback.DisguiseCallback;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        disguiseHandler.unDisguise(player, true, result -> {
            if (result == DisguiseCallback.SUCCESS) {
                send(player, "disguise.undisguised", "&aYou are no longer disguised.");
            } else if (result == DisguiseCallback.NOT_DISGUISED) {
                send(player, "disguise.not-disguised", "&cYou are not disguised.");
            } else {
                send(player, "disguise.error", "&cAn error occurred while undisguising.");
            }
        });
    }
}