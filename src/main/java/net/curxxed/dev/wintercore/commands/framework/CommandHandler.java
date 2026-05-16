package net.curxxed.dev.wintercore.commands.framework;

import lombok.SneakyThrows;
import net.curxxed.dev.wintercore.plugin.WinterCore;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CommandHandler {

    private final WinterCore plugin;
    private final CommandMap commandMap;
    private final List<BaseCommand> registeredCommands = new ArrayList<>();
    private final Set<String> registeredPermissionNodes = new LinkedHashSet<>();

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

        String[] permissions = info.permission();
        if (permissions.length == 1) {
            pluginCommand.setPermission(permissions[0]);
        } else {
            pluginCommand.setPermission(null);
        }
        
        pluginCommand.setPermissionMessage(plugin.getMessageConfig().get("general.no-permission",
                "&cYou do not have permission to use this command."));

        commandMap.register(plugin.getDescription().getName(), pluginCommand);
        registeredCommands.add(commandExecutor);
        collectPermissionNodes(permissions);
    }

    public List<BaseCommand> getRegisteredCommands() {
        return Collections.unmodifiableList(registeredCommands);
    }

    public Set<String> getRegisteredPermissionNodes() {
        return Collections.unmodifiableSet(registeredPermissionNodes);
    }

    private void collectPermissionNodes(String[] permissions) {
        for (String node : permissions) {
            String normalized = normalizePermissionNode(node);
            if (!normalized.isEmpty()) {
                registeredPermissionNodes.add(normalized);
            }
        }
    }

    private String normalizePermissionNode(String node) {
        if (node == null) {
            return "";
        }
        return node.trim().toLowerCase(Locale.ENGLISH);
    }
}
