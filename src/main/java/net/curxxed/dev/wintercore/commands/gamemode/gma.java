package net.curxxed.dev.wintercore.commands.gamemode;

import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class gma extends BaseCommand {

    private final StaffModeManager staffModeManager;

    public gma(StaffModeManager staffModeManager) {
        this.staffModeManager = staffModeManager;
    }

    @Command(
            name = "gma",
            permission = "WinterCore.gamemode",
            description = "Set yourself or another player to Adventure mode.",
            usage = "/gma [player]",
            aliases = "gm2",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();

        if (staffModeManager != null && staffModeManager.isInStaffMode(player)) {
            player.sendMessage(CC.translate("&cYou cannot change your game mode while in staff mode!"));
            return;
        }

        Player target;
        if (commandArgs.length() > 0) {
            target = Bukkit.getPlayer(commandArgs.getArgs(0));
            if (target == null || !target.isOnline()) {
                player.sendMessage(CC.translate("&cPlayer not found!"));
                return;
            }
        } else {
            target = player;
        }

        target.setGameMode(GameMode.ADVENTURE);
        target.sendMessage(CC.translate("&bYour game mode has been set to Adventure Mode."));
        if (!target.equals(player)) {
            player.sendMessage(CC.translate("&aSet " + target.getName() + "'s game mode to Adventure."));
        }
    }
}