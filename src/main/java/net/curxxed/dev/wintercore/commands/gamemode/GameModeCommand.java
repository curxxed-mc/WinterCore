package net.curxxed.dev.wintercore.commands.gamemode;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(
        name = "gamemode",
        aliases = {"gm", "gmc", "gm1", "gms", "gm0", "gma", "gm2", "gmsp", "gm3"},
        permission = "wintercore.command.gamemode",
        usage = "/gamemode <mode> [player]",
        description = "Changes your or another player's gamemode."
)
public class GameModeCommand extends BaseCommand {

    private final StaffModeManager staffModeManager;

    public GameModeCommand(WinterCore plugin, StaffModeManager staffModeManager) {
        super(plugin);
        this.staffModeManager = staffModeManager;
    }

    @Override
    public void execute(CommandArguments args) {
        CommandSender sender = args.getSender();
        String label = args.getLabel().toLowerCase();
        GameMode targetGameMode = null;
        String modeInput = args.getOptionalString(0).orElse("");
        if (!modeInput.isEmpty()) {
            targetGameMode = parseGameMode(modeInput);
        }

        if (targetGameMode == null) {
            switch (label) {
                case "gmc": case "gm1":
                    targetGameMode = GameMode.CREATIVE;
                    break;
                case "gms": case "gm0":
                    targetGameMode = GameMode.SURVIVAL;
                    break;
                case "gma": case "gm2":
                    targetGameMode = GameMode.ADVENTURE;
                    break;
                case "gmsp": case "gm3":
                    targetGameMode = GameMode.SPECTATOR;
                    break;
            }
        }

        if (targetGameMode == null) {
            sender.sendMessage(CC.translate("&cInvalid gamemode specified. Use creative, survival, adventure, or spectator."));
            return;
        }

        // Step 2: Determine the target Player
        Player targetPlayer = null;
        // If mode was parsed from arg 0, player might be in arg 1
        if (parseGameMode(modeInput) != null) {
            targetPlayer = args.getOptionalPlayer(1).orElse(null);
        } else { // Otherwise, player might be in arg 0
            targetPlayer = args.getOptionalPlayer(0).orElse(null);
        }

        // If no player specified in args, default to sender
        if (targetPlayer == null) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage(CC.translate("&cConsole must specify a player."));
                return;
            }
        }

        // Step 3: Perform permission and safety checks
        if (!sender.hasPermission("wintercore.command.gamemode." + targetGameMode.name().toLowerCase())) {
            sender.sendMessage(CC.translate("&cYou do not have permission to set this gamemode."));
            return;
        }

        if (sender != targetPlayer && !sender.hasPermission("wintercore.command.gamemode.others")) {
            sender.sendMessage(CC.translate("&cYou do not have permission to change other players' gamemode."));
            return;
        }

        if (staffModeManager != null && staffModeManager.isInStaffMode(targetPlayer)) {
            sender.sendMessage(CC.translate("&cYou cannot change that player's game mode while they are in staff mode!"));
            return;
        }

        // Step 4: Execute the command and send feedback
        targetPlayer.setGameMode(targetGameMode);

        String modeName = targetGameMode.name().substring(0, 1).toUpperCase() + targetGameMode.name().substring(1).toLowerCase();

        if (sender == targetPlayer) {
            sender.sendMessage(CC.translate("&bYour gamemode has been set to " + modeName + "."));
        } else {
            sender.sendMessage(CC.translate("&bSet " + targetPlayer.getName() + "'s gamemode to " + modeName + "."));
            targetPlayer.sendMessage(CC.translate("&bYour gamemode has been set to " + modeName + " by " + sender.getName() + "."));
        }
    }

    private GameMode parseGameMode(String input) {
        switch (input.toLowerCase()) {
            case "creative": case "c": case "1":
                return GameMode.CREATIVE;
            case "survival": case "s": case "0":
                return GameMode.SURVIVAL;
            case "adventure": case "a": case "2":
                return GameMode.ADVENTURE;
            case "spectator": case "sp": case "3":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
}
