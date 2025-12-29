package net.curxxed.dev.wintercore.commands.api;

import lombok.SneakyThrows;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Handles the dynamic registration of commands based on the @CommandInfo annotation.
 * This class uses reflection to interact with Bukkit's internal command map.
 */
public class CommandHandler {

    private final WinterCore plugin;
    private final CommandMap commandMap;

    @SneakyThrows
    public CommandHandler(WinterCore plugin) {
        this.plugin = plugin;
        SimplePluginManager spm = (SimplePluginManager) Bukkit.getPluginManager();
        Field field = spm.getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        this.commandMap = (CommandMap) field.get(spm);
    }

    /**
     * Registers a single command class with Bukkit's command map.
     * @param commandClass The class of the command to register (must extend BaseCommand).
     */
    @SneakyThrows
    public void register(Class<? extends BaseCommand> commandClass) {
        CommandInfo info = commandClass.getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().severe("Could not register command " + commandClass.getName() + ", it is missing @CommandInfo annotation.");
            return;
        }

        try {
            // Step 1: Create an instance of our command executor
            Constructor<? extends BaseCommand> constructor = commandClass.getConstructor(WinterCore.class);
            BaseCommand commandExecutor = constructor.newInstance(plugin);

            // Step 2: Create a new PluginCommand (the Bukkit wrapper for our command)
            Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            pluginCommandConstructor.setAccessible(true);
            PluginCommand pluginCommand = pluginCommandConstructor.newInstance(info.name(), plugin);

            // Step 3: Set the properties from our annotation
            pluginCommand.setExecutor(commandExecutor);
            pluginCommand.setAliases(Arrays.asList(info.aliases()));
            pluginCommand.setDescription(info.description());
            pluginCommand.setUsage(info.usage());
            pluginCommand.setPermission(info.permission());
            pluginCommand.setPermissionMessage(CC.translate("&cYou do not have permission to use this command."));

            // Step 4: Register the command
            commandMap.register(plugin.getDescription().getName(), pluginCommand);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + info.name());
            e.printStackTrace();
        }
    }

    /**
     * Registers a pre-constructed BaseCommand instance. This is useful for commands that require
     * constructor parameters other than the plugin or for commands that also implement Listener
     * and need to be registered as a single instance for both command execution and event handling.
     *
     * @param commandInstance The instantiated command to register.
     */
    @SneakyThrows
    public void register(BaseCommand commandInstance) {
        CommandInfo info = commandInstance.getClass().getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().severe("Could not register command instance " + commandInstance.getClass().getName() + ", it is missing @CommandInfo annotation.");
            return;
        }

        try {
            // Create the PluginCommand wrapper using reflection
            Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            pluginCommandConstructor.setAccessible(true);
            PluginCommand pluginCommand = pluginCommandConstructor.newInstance(info.name(), plugin);

            // Configure the plugin command
            pluginCommand.setExecutor(commandInstance);
            pluginCommand.setAliases(Arrays.asList(info.aliases()));
            pluginCommand.setDescription(info.description());
            pluginCommand.setUsage(info.usage());
            pluginCommand.setPermission(info.permission());
            pluginCommand.setPermissionMessage(CC.translate("&cYou do not have permission to use this command."));

            // Register
            commandMap.register(plugin.getDescription().getName(), pluginCommand);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command instance: " + info.name());
            e.printStackTrace();
        }
    }
}
