package net.curxxed.dev.wintercore.commands.social;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "discord",
            description = "Get the Discord server link.",
            usage = "/discord",
            inGameOnly = true
    
    )
public class DiscordCommand extends BaseCommand {

    private final String discordLink = ChatColor.DARK_PURPLE + "discord.gg/wintercore";

    public DiscordCommand(WinterCore plugin) {
        super(plugin);
    }

    @Override

    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();
        player.sendMessage(ChatColor.AQUA + "Join our discord: " + discordLink);
    }
}