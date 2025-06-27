package net.curxxed.dev.wintercore.commands.troll;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class idontknowwhatthisisdontlook extends BaseCommand {

    @Command(
            name = "idkwtidl",
            description = "Troll command to fake your ping.",
            usage = "/idkwtidl <set|add|decrease|reset> [value]",
            inGameOnly = true
    )
    public void onCommand(CommandArgs commandArgs) {
        Player player = commandArgs.getPlayer();
        String[] args = commandArgs.getArgs();

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /idkwtidl <set|add|decrease|reset> [value]");
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /idkwtidl set <value>");
                    return;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    setFakePing(player, value);
                    player.sendMessage(ChatColor.GREEN + "Your fake ping has been set to " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid value. Please enter a valid number.");
                }
                break;

            case "add":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /idkwtidl add <value>");
                    return;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    int currentPing = Utilities.getPing(player);
                    setFakePing(player, currentPing + value);
                    player.sendMessage(ChatColor.GREEN + "Your fake ping has been increased by " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid value. Please enter a valid number.");
                }
                break;

            case "decrease":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /idkwtidl decrease <value>");
                    return;
                }
                try {
                    int value = Integer.parseInt(args[1]);
                    int currentPing = Utilities.getPing(player);
                    setFakePing(player, currentPing - value);
                    player.sendMessage(ChatColor.GREEN + "Your fake ping has been decreased by " + value + " ms.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid value. Please enter a valid number.");
                }
                break;

            case "reset":
                resetPing(player);
                player.sendMessage(ChatColor.GREEN + "Your fake ping has been reset to normal.");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /idkwtidl <set|add|decrease|reset> [value]");
                break;
        }
    }

    private void setFakePing(Player player, int ping) {
        try {
            Object handle = Utilities.getEntityPlayer(player);
            if (Utilities.IS_LEGACY) {
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
            Object handle = Utilities.getEntityPlayer(player);
            int actualPing = Utilities.getPing(player);
            if (Utilities.IS_LEGACY) {
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