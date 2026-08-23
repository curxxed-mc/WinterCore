package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "heal",
        description = "Heal yourself or another player.",
        usage = "/heal [player]",
        inGameOnly = false,
        permission = {"wintercore.heal", "WinterCore.heal"}
)
public class Heal extends BaseCommand {

    public Heal(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player senderPlayer = commandArgs.getPlayer();
        Player target;

        if (commandArgs.length() > 0) {
            target = Bukkit.getPlayer(commandArgs.getArgs()[0]);
            if (target == null || !target.isOnline()) {
                send(commandArgs.getSender(), "general.player-not-found", "&cPlayer not found!");
                return;
            }
        } else if (commandArgs.isPlayer()) {
            target = senderPlayer;
        } else {
            send(commandArgs.getSender(), "heal.self-player-only", "&cOnly players can use this command on themselves!");
            return;
        }

        target.setHealth(20);
        send(target, "heal.target-success", "&aYour health has been restored!");
        if (!target.equals(commandArgs.getSender())) {
            send(commandArgs.getSender(), "heal.actor-success", "&aYou have healed {target}!",
                    "{target}", target.getName());
        }
    }
}
