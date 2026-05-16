package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "freeze",
        description = "Freeze or unfreeze player",
        usage = "/freeze <player>",
        inGameOnly = true,
        permission = {"wintercore.commands.freeze", "WinterCore.commands.freeze"}
)
public class FreezeCommand extends BaseCommand {
    private final FreezeListener freezeListener;

    public FreezeCommand(FreezeListener freezeListener, WinterCore plugin) {
        super(plugin);
        this.freezeListener = freezeListener;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (commandArgs.length() != 1) {
            sendUsage(player);
            return;
        }

        Player target = commandArgs.getOptionalPlayer(0).orElse(null);
        if (target == null) {
            send(player, "general.player-not-found", "&cPlayer not found.");
            return;
        }

        if (freezeListener.isFrozen(target)) {
            freezeListener.unfreezePlayer(target, player);
            send(player, "moderation.freeze.actor-unfrozen",
                    "&aUnfroze {target}.",
                    "{target}", target.getName());
            send(target, "moderation.freeze.target-unfrozen",
                    "&aYou have been unfrozen.");
        } else {
            freezeListener.freezePlayer(target, player);
            String discordLink = plugin.getConfig().getString("discord-link", "discord.gg/example");
            send(player, "moderation.freeze.actor-frozen",
                    "&cFroze {target}.",
                    "{target}", target.getName());
            send(target, "moderation.freeze.target-frozen",
                    "&cYou have been frozen! Join our Discord for more information. &b{discord}",
                    "{discord}", discordLink);
        }
    }
}





