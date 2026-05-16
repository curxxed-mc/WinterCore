package net.curxxed.dev.wintercore.commands.social;

import net.curxxed.dev.wintercore.commands.framework.BaseCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandInfo;
import net.curxxed.dev.wintercore.commands.framework.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "discord",
        description = "Get the Discord server link.",
        usage = "/discord",
        inGameOnly = true,
        permission = {}
)
public class DiscordCommand extends BaseCommand {

    public DiscordCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        send(player, "discord.message", "&bJoin our discord: &5{link}",
                "{link}", msg("discord.link", "discord.gg/wintercore"));
    }
}
