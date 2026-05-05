package net.curxxed.dev.wintercore.commands.gamemode;

import net.curxxed.dev.wintercore.commands.api.BaseCommand;
import net.curxxed.dev.wintercore.commands.api.CommandArguments;
import net.curxxed.dev.wintercore.commands.api.CommandInfo;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.staff.StaffModeManager;
import net.curxxed.dev.wintercore.utils.CC;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@CommandInfo(
        name = "gamemode",
        aliases = {"gm", "gmc", "gm1", "gms", "gm0", "gma", "gm2", "gmsp", "gm3"},
        usage = "/gamemode <mode> [player]",
        description = "Changes your or another player's gamemode.",
        permission = {"wintercore.command.gamemode"}
)
public class GameModeCommand extends BaseCommand {

    private final StaffModeManager staffModeManager;
    private static final List<String> MODE_TOKENS = Arrays.asList(
            "creative", "survival", "adventure", "spectator",
            "c", "s", "a", "sp", "1", "0", "2", "3"
    );

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
                    if (Utilities.IS_1_7) {
                        sender.sendMessage(CC.translate("&cSpectator mode is not available in Minecraft 1.7.10."));
                        return;
                }
                    targetGameMode = GameMode.SPECTATOR;
                    break;
            }
        }

        if (targetGameMode == null) {
            sender.sendMessage(CC.translate("&cInvalid gamemode specified. Use creative, survival, adventure, or spectator."));
            return;
        }
        Player targetPlayer;
        if (parseGameMode(modeInput) != null) {
            targetPlayer = args.getOptionalPlayer(1).orElse(null);
        } else {
            targetPlayer = args.getOptionalPlayer(0).orElse(null);
        }

        if (targetPlayer == null) {
            if (sender instanceof Player) {
                targetPlayer = (Player) sender;
            } else {
                sender.sendMessage(CC.translate("&cConsole must specify a player."));
                return;
            }
        }

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

        targetPlayer.setGameMode(targetGameMode);

        String modeName = targetGameMode.name().substring(0, 1).toUpperCase() + targetGameMode.name().substring(1).toLowerCase();

        if (sender == targetPlayer) {
            sender.sendMessage(CC.translate("&bYour gamemode has been set to " + modeName + "."));
        } else {
            sender.sendMessage(CC.translate("&bSet " + targetPlayer.getName() + "'s gamemode to " + modeName + "."));
            targetPlayer.sendMessage(CC.translate("&bYour gamemode has been set to " + modeName + " by " + sender.getName() + "."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandArguments args) {
        GameMode forcedMode = modeFromAlias(args.getLabel());
        if (forcedMode != null) {
            if (args.length() == 1) {
                return completeOnlinePlayers(args);
            }
            return Collections.emptyList();
        }

        if (args.length() == 1) {
            return completeCurrentArg(args, MODE_TOKENS);
        }

        if (args.length() == 2) {
            String first = args.getOptionalString(0).orElse("");
            if (parseGameMode(first) != null) {
                return completeOnlinePlayers(args);
            }
        }

        return Collections.emptyList();
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

    private GameMode modeFromAlias(String label) {
        switch (label.toLowerCase(Locale.ENGLISH)) {
            case "gmc":
            case "gm1":
                return GameMode.CREATIVE;
            case "gms":
            case "gm0":
                return GameMode.SURVIVAL;
            case "gma":
            case "gm2":
                return GameMode.ADVENTURE;
            case "gmsp":
            case "gm3":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
}
