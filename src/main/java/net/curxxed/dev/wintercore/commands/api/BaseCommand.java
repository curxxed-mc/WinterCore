package net.curxxed.dev.wintercore.commands.api;

import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements TabExecutor {

    protected final WinterCore plugin;
    @Getter
    protected final CommandInfo commandInfo;

    public BaseCommand(WinterCore plugin) {
        this.plugin = plugin;
        this.commandInfo = getClass().getAnnotation(CommandInfo.class);
        if (this.commandInfo == null) {
            throw new IllegalStateException("Command class " + getClass().getName() + " is missing the @CommandInfo annotation!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (commandInfo.permission().length > 0 && !hasPermission(sender, commandInfo.permission())) {
            sender.sendMessage(CC.translate("&cYou do not have permission to execute this command."));
            return true;
        }

        if (commandInfo.inGameOnly() && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(CC.translate("&cThis command can only be executed by players."));
            return true;
        }

        final CommandArguments commandArgs = new CommandArguments(sender, args, label);
        if (commandInfo.async()) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.execute(commandArgs));
        } else {
            this.execute(commandArgs);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (commandInfo.permission().length > 0 && !hasPermission(sender, commandInfo.permission())) {
            return Collections.emptyList();
        }
        if (commandInfo.inGameOnly() && !(sender instanceof org.bukkit.entity.Player)) {
            return Collections.emptyList();
        }
        return onTabComplete(new CommandArguments(sender, args, label));
    }

    private boolean hasPermission(CommandSender sender, String[] permissions) {
        if (permissions.length == 0) {
            return true;
        }
        return Arrays.stream(permissions).anyMatch(sender::hasPermission);
    }

    public abstract void execute(CommandArguments args);

    public List<String> onTabComplete(CommandArguments args) {
        return Collections.emptyList();
    }
}