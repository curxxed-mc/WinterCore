package net.curxxed.dev.wintercore.commands.gamemode;

import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class gmsp extends BaseCommand {

    private final StaffModeManager staffModeManager;

    public gmsp(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Command(
            name = "gmsp",
            permission = "WinterCore.gamemode",
            description = "Set yourself or another player to Spectator mode.",
            usage = "/gmsp [player]",
            aliases = "gm3",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (staffModeManager != null && staffModeManager.isInStaffMode(player)) {
            player.sendMessage(ChatColor.RED + "You cannot change your game mode while in staff mode!");
            return;
        }

        Player target;
        if (commandArgs.length() > 0) {
            target = Bukkit.getPlayer(commandArgs.getArgs(0));
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player not found!");
                return;
            }
        } else {
            target = player;
        }

        target.setGameMode(GameMode.SPECTATOR);
        target.sendMessage(ChatColor.AQUA + "Your game mode has been set to Spectator Mode.");
        if (!target.equals(player)) {
            player.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s game mode to Spectator.");
        }
    }
}