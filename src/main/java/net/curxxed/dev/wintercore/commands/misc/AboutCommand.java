package net.curxxed.dev.wintercore.commands.misc;

import net.curxxed.dev.CommandAPI.BaseCommand;
import net.curxxed.dev.CommandAPI.Command;
import net.curxxed.dev.CommandAPI.CommandArgs;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.plugin.java.JavaPlugin;

public class AboutCommand extends BaseCommand {
    private final JavaPlugin plugin;

    public AboutCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(
        name = "about",
        permission = "WinterCore.about",
        description = "Show plugin information.",
        usage = "/about",
        inGameOnly = false
    )
    public void onCommand(CommandArgs commandArgs) {
        String version = plugin.getDescription().getVersion();
        commandArgs.getSender().sendMessage(CC.translate("&7&m----------------------------"));
        commandArgs.getSender().sendMessage(CC.translate("&bPlugin Name: &f" + "WinterCore"));
        commandArgs.getSender().sendMessage(CC.translate("&bVersion: &f" + version));
        commandArgs.getSender().sendMessage(CC.translate("&bAuthor: &fCurxxed"));
        commandArgs.getSender().sendMessage(CC.translate("&bDescription: &e" +  plugin.getDescription().getDescription()));
        commandArgs.getSender().sendMessage(CC.translate("&bDiscord: &f@curxxe"));
        commandArgs.getSender().sendMessage(CC.translate("&7&m----------------------------"));
    }
}

