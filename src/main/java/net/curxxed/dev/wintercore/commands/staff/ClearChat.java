package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "clearchat",
            permission = "WinterCore.clearchat",
            description = "Clear the chat for all players.",
            usage = "/clearchat",
            inGameOnly = false
    )
public class ClearChat extends BaseCommand {

    public ClearChat(WinterCore plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 200; i++) {
                player.sendMessage("");
            }
        }
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "Chat has been cleared.");
    }
}