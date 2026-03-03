package net.curxxed.dev.wintercore.commands.api;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * The abstract blueprint for all commands in the framework.
 * It handles permission checks, async execution, and forces a consistent structure.
 */
public abstract class BaseCommand implements CommandExecutor {

    protected final WinterCore plugin;
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
        if (!commandInfo.permission().isEmpty() && !sender.hasPermission(commandInfo.permission())) {
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

    /**
     * The core logic of the command. This method must be implemented by all concrete command classes.
     *
     * @param args The wrapped command arguments and sender.
     */
    public abstract void execute(CommandArguments args);
}
