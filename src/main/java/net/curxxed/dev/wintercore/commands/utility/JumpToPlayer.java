package net.curxxed.dev.wintercore.commands.utility;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "jtp",
        description = "Jump to another player.",
        usage = "/jtp <player>",
        inGameOnly = true,
        permission = {"wintercore.jtp", "WinterCore.jtp"}
)
public class JumpToPlayer extends BaseCommand {

    public JumpToPlayer(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player p = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length != 1) {
            sendUsage(p);
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(p, "general.player-not-found", "&cPlayer not found.");
            return;
        }

        p.teleport(target.getLocation());
        send(p, "jump-to-player.success", "&aJumped to {target}.",
                "{target}", target.getName());
    }
}
