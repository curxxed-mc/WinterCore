package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "feed",
        description = "Feed yourself or another player.",
        usage = "/feed [player]",
        inGameOnly = false,
        permission = {"wintercore.feed", "WinterCore.feed"}
)
public class Feed extends BaseCommand {

    public Feed(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        if (commandArgs.length() == 0) {
            if (!commandArgs.isPlayer()) {
                send(commandArgs.getSender(), "feed.self-player-only", "&cOnly players can use this command on themselves!");
                return;
            }
            Player player = commandArgs.getPlayer();
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            send(player, "feed.self-success", "&aYou are now full.");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null || !target.isOnline()) {
            send(commandArgs.getSender(), "general.player-not-found", "&cPlayer not found!");
            return;
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        send(target, "feed.target-success", "&aYou have been fed by {sender}.",
                "{sender}", commandArgs.getSender().getName());
        send(commandArgs.getSender(), "feed.actor-success", "&aYou have fed {target}.",
                "{target}", target.getName());
    }
}
