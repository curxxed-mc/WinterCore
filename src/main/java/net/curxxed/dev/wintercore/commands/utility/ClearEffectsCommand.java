package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
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
                sender.sendMessage(CC.RED + "You do not have permission to clear everyone's effects.");
                return;
            }
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                for (PotionEffect effect : onlinePlayer.getActivePotionEffects()) {
                    onlinePlayer.removePotionEffect(effect.getType());
                }
                onlinePlayer.sendMessage(CC.GREEN + "All your potion effects have been cleared.");
            }
            sender.sendMessage(CC.GREEN + "You cleared all players' potion effects.");
            return;
        }

        Player target = sender;
        if (args.length == 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(CC.RED + "Player not found.");
                return;
            }
        }

        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }
        target.sendMessage(CC.GREEN + "Your potion effects have been cleared.");
        if (!target.equals(sender)) {
            sender.sendMessage(CC.GREEN + "You cleared " + target.getName() + "'s potion effects.");
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





