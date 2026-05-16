package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(
        name = "cleareffects",
        description = "Clear potion effects from yourself or another player.",
        usage = "/cleareffects [player|@a]",
        inGameOnly = true,
        permission = {"wintercore.cleareffects"}
)
public class ClearEffectsCommand extends BaseCommand {

    public ClearEffectsCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player sender = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length == 1 && args[0].equalsIgnoreCase("@a")) {
            if (!sender.hasPermission("wintercore.cleareffects.all")) {
                send(sender, "clear-effects.no-all-permission", "&cYou do not have permission to clear everyone's effects.");
                return;
            }
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                for (PotionEffect effect : onlinePlayer.getActivePotionEffects()) {
                    onlinePlayer.removePotionEffect(effect.getType());
                }
                send(onlinePlayer, "clear-effects.target-success", "&aYour potion effects have been cleared.");
            }
            send(sender, "clear-effects.all-success", "&aYou cleared all players' potion effects.");
            return;
        }

        Player target = sender;
        if (args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                send(sender, "general.player-not-found", "&cPlayer not found.");
                return;
            }
        }

        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }
        send(target, "clear-effects.target-success", "&aYour potion effects have been cleared.");
        if (!target.equals(sender)) {
            send(sender, "clear-effects.actor-success", "&aYou cleared {target}'s potion effects.",
                    "{target}", target.getName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        if (args.length() == 1) {
            List<String> candidates = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            if (args.getSender().hasPermission("wintercore.cleareffects.all")) {
                candidates.add("@a");
            }
            return completeCurrentArg(args, candidates);
        }

        return Collections.emptyList();
    }
}





