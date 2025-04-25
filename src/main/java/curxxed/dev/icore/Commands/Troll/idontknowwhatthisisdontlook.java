package curxxed.dev.icore.Commands.Troll;

import curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class idontknowwhatthisisdontlook implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("Usage: /idkwtidl <set|add|decrease|reset> [value]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                if (args.length < 2) {
                    player.sendMessage("Usage: /idkwtidl set <value>");
                    return true;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    setFakePing(player, value);
                    player.sendMessage("Your fake ping has been set to " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid value. Please enter a valid number.");
                }
                break;

            case "add":
                if (args.length < 2) {
                    player.sendMessage("Usage: /idkwtidl add <value>");
                    return true;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    int currentPing = NMSUtils.getPing(player);
                    setFakePing(player, currentPing + value);
                    player.sendMessage("Your fake ping has been increased by " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid value. Please enter a valid number.");
                }
                break;

            case "decrease":
                if (args.length < 2) {
                    player.sendMessage("Usage: /idkwtidl decrease <value>");
                    return true;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    int currentPing = NMSUtils.getPing(player);
                    setFakePing(player, currentPing - value);
                    player.sendMessage("Your fake ping has been decreased by " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid value. Please enter a valid number.");
                }
                break;

            case "reset":
                resetPing(player);
                player.sendMessage("Your fake ping has been reset to normal.");
                break;

            default:
                player.sendMessage("Unknown subcommand. Usage: /idkwtidl <set|add|decrease|reset> [value]");
                break;
        }

        return true;
    }

    private void setFakePing(Player player, int ping) {
        try {
            Object handle = NMSUtils.getEntityPlayer(player);
            if (NMSUtils.IS_LEGACY) {
                Field pingField = handle.getClass().getField("ping");
                pingField.setInt(handle, ping);
            } else {
                Method setPing = handle.getClass().getMethod("setPing", int.class);
                setPing.invoke(handle, ping);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetPing(Player player) {
        try {
            Object handle = NMSUtils.getEntityPlayer(player);
            int actualPing = NMSUtils.getPing(player);
            if (NMSUtils.IS_LEGACY) {
                Field pingField = handle.getClass().getField("ping");
                pingField.setInt(handle, actualPing);
            } else {
                Method setPing = handle.getClass().getMethod("setPing", int.class);
                setPing.invoke(handle, actualPing);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}