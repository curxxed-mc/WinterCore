package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ClearChat extends BaseCommand {

    @Command(
            name = "clearchat",
            permission = "iCore.clearchat",
            description = "Clear the chat for all players.",
            usage = "/clearchat",
            inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 200; i++) {
                player.sendMessage("");
            }
        }
        commandArgs.getSender().sendMessage(ChatColor.GREEN + "Chat has been cleared.");
    }
}