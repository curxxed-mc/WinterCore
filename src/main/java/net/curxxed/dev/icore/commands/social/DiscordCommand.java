package net.curxxed.dev.icore.commands.social;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DiscordCommand extends BaseCommand {

    private final String discordLink = ChatColor.DARK_PURPLE + "discord.gg/icore";

    @Command(
            name = "discord",
            description = "Get the Discord server link.",
            usage = "/discord",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        player.sendMessage(ChatColor.AQUA + "Join our discord: " + discordLink);
    }
}