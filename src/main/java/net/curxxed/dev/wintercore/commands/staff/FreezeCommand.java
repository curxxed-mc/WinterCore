package net.curxxed.dev.wintercore.commands.staff;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "freeze",
            permission = "WinterCore.commands.freeze",
            description = "Freeze or unfreeze player",
            usage = "/freeze <player>",
            inGameOnly = true
    )
public class FreezeCommand extends BaseCommand {
    private final FreezeListener freezeListener;
    private final String discordLink = "discord.gg/example";

    public FreezeCommand(FreezeListener freezeListener, WinterCore plugin) {
        super(plugin);
        this.freezeListener = freezeListener;
    }

    @Override
    public void execute(CommandArguments commandArgs) {
        Player player = commandArgs.getPlayer();

        if (!player.hasPermission("WinterCore.freeze")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (commandArgs.length() != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /freeze <player>");
            return;
        }

        Player target = commandArgs.getOptionalPlayer(0).orElse(null);
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