package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "fly",
        description = "Toggle flight for yourself or another player.",
        usage = "/fly [player]",
        inGameOnly = true,
        permission = {"wintercore.fly", "WinterCore.fly"}
)
public class Fly extends BaseCommand {

    public Fly(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player senderPlayer = commandArgs.getPlayer();

        if (commandArgs.length() == 0) {

            toggleFlight(senderPlayer, senderPlayer);
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
        if (target == null) {
            send(commandArgs.getSender(), "general.player-not-found", "&cPlayer not found!");
            return;
        }

        toggleFlight(commandArgs.getSender(), target);
    }

    private void toggleFlight(org.bukkit.command.CommandSender sender, Player target) {
        target.setAllowFlight(!target.getAllowFlight());

        if (target.getAllowFlight()) {
            send(target, "fly.enabled", "&aYou can now fly!");
            if (!target.equals(sender)) {
                send(sender, "fly.enabled-other", "&a{target} can now fly!",
                        "{target}", target.getName());
            }
        } else {
            send(target, "fly.disabled", "&cYou can no longer fly!");
            if (!target.equals(sender)) {
                send(sender, "fly.disabled-other", "&c{target} can no longer fly!",
                        "{target}", target.getName());
            }
        }
    }
}
