package net.curxxed.dev.icore.commands.staff;

import net.curxxed.dev.icore.listeners.FreezeListener;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class FreezeCommand extends BaseCommand {
    private final FreezeListener freezeListener;
    private final String discordLink = "discord.gg/example";

    public FreezeCommand(FreezeListener freezeListener) {
        this.freezeListener = freezeListener;
    }
    @Command(name = "freeze",
            permission = "iCore.commands.freeze",
            description = "Freeze or unfreeze a player",
            usage = "/freeze <player>",
            inGameOnly = true)
    @Override
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!player.hasPermission("iCore.freeze")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (commandArgs.length() != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /freeze <player>");
            return;
        }

        Player target = Bukkit.getPlayer(commandArgs.getArgs(0));
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (freezeListener.isFrozen(target)) {
            freezeListener.unfreezePlayer(target, player);
            player.sendMessage(ChatColor.GREEN + "Unfroze " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "You have been unfrozen.");
        } else {
            freezeListener.freezePlayer(target, player);
            player.sendMessage(ChatColor.RED + "Froze " + target.getName() + ".");
            target.sendMessage(ChatColor.RED + "You have been frozen! Join our Discord for more information." + ChatColor.AQUA + discordLink);
        }
    }
}