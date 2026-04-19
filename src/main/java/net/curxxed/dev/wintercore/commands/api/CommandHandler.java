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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandHandler {

    private final WinterCore plugin;
    private final CommandMap commandMap;
    private final List<BaseCommand> registeredCommands = new ArrayList<>();

    @SneakyThrows
    public CommandHandler(WinterCore plugin) {
        this.plugin = plugin;
        SimplePluginManager spm = (SimplePluginManager) Bukkit.getPluginManager();
        Field field = spm.getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        this.commandMap = (CommandMap) field.get(spm);
    }

    public void register(Class<? extends BaseCommand> commandClass) {
        CommandInfo info = commandClass.getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().severe("Could not register command " + commandClass.getName() + ", it is missing @CommandInfo annotation.");
            return;
        }

        try {
            Constructor<? extends BaseCommand> constructor = commandClass.getConstructor(WinterCore.class);
            BaseCommand commandExecutor = constructor.newInstance(plugin);
            registerInternal(commandExecutor, info);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command: " + info.name());
            e.printStackTrace();
        }
    }

    public void register(BaseCommand commandInstance) {
        CommandInfo info = commandInstance.getClass().getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().severe("Could not register command instance " + commandInstance.getClass().getName() + ", it is missing @CommandInfo annotation.");
            return;
        }

        try {
            registerInternal(commandInstance, info);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command instance: " + info.name());
            e.printStackTrace();
        }
    }

    private void registerInternal(BaseCommand commandExecutor, CommandInfo info) throws Exception {
        Constructor<PluginCommand> pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        pluginCommandConstructor.setAccessible(true);
        PluginCommand pluginCommand = pluginCommandConstructor.newInstance(info.name(), plugin);

        pluginCommand.setExecutor(commandExecutor);
        pluginCommand.setTabCompleter(commandExecutor);
        pluginCommand.setAliases(Arrays.asList(info.aliases()));
        pluginCommand.setDescription(info.description());
        pluginCommand.setUsage(info.usage());
        
        // Join permission array with space for Bukkit API compatibility
        String[] permissions = info.permission();
        if (permissions.length > 0) {
            pluginCommand.setPermission(String.join(" ", permissions));
        }
        
        pluginCommand.setPermissionMessage(CC.translate("&cYou do not have permission to use this command."));

        commandMap.register(plugin.getDescription().getName(), pluginCommand);
        registeredCommands.add(commandExecutor);
    }

    public List<BaseCommand> getRegisteredCommands() {
        return Collections.unmodifiableList(registeredCommands);
    }
}