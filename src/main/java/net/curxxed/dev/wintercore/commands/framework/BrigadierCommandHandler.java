package net.curxxed.dev.wintercore.commands.framework;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

public class BrigadierCommandHandler {
    private final WinterCore plugin;
    private final List<BaseCommand> commands;
    // Someone said this is the equivalent of a C struct definition but instead of the compiler checking it,
    //we're just hoping these classes exist at runtime.
    private final Class<?> literalBuilderClass;
    private final Class<?> greedyStringClass;
    private final Class<?> requiredBuilderClass;
    private final Class<?> brigadierCommandClass;
    private final Class<?> predClass;
    private final Class<?> suggestionProviderClass;
    private final Class<?> commandNodeClass;
    private final Class<?> argumentTypeClass;

    private final Class<?> lifecycleEventsClass;
    private final Class<?> lifecycleHandlerClass;
    private final Class<?> commandsRegistrarClass;

    public BrigadierCommandHandler(WinterCore plugin, List<BaseCommand> commands) throws ClassNotFoundException {
        this.plugin   = plugin;
        this.commands = commands;

        literalBuilderClass     = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
        greedyStringClass       = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
        requiredBuilderClass    = Class.forName("com.mojang.brigadier.builder.RequiredArgumentBuilder");
        brigadierCommandClass   = Class.forName("com.mojang.brigadier.Command");
        predClass               = Class.forName("java.util.function.Predicate");
        suggestionProviderClass = Class.forName("com.mojang.brigadier.suggestion.SuggestionProvider");
        commandNodeClass        = Class.forName("com.mojang.brigadier.tree.CommandNode");
        argumentTypeClass       = Class.forName("com.mojang.brigadier.arguments.ArgumentType");

        lifecycleEventsClass    = Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");
        lifecycleHandlerClass   = Class.forName("io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler");
        commandsRegistrarClass  = Class.forName("io.papermc.paper.command.brigadier.Commands");
    }

    public void initialize() {
        try {
            Object commandsEventType = lifecycleEventsClass.getField("COMMANDS").get(null);

            Object handler = Proxy.newProxyInstance(
                    plugin.getClass().getClassLoader(),
                    new Class[]{lifecycleHandlerClass},
                    (proxy, method, args) -> {
                        if ("run".equals(method.getName()) && args != null && args.length == 1) {
                            onCommandsEvent(args[0]);
                        }
                        return null;
                    }
            );

            Object lifecycleManager = plugin.getClass()
                    .getMethod("getLifecycleManager")
                    .invoke(plugin);

            // registerEventHandler(LifecycleEventType, LifecycleEventHandler)
            for (Method m : lifecycleManager.getClass().getMethods()) {
                if ("registerEventHandler".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(lifecycleManager, commandsEventType, handler);
                    break;
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[WinterCore] Failed to initialize Brigadier registration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void onCommandsEvent(Object event) throws Exception {
        // Pull registrar from the event (Commands registrar())
        Object registrar = null;
        for (Method m : event.getClass().getMethods()) {
            if ("registrar".equals(m.getName()) && m.getParameterCount() == 0) {
                registrar = m.invoke(event);
                break;
            }
        }
        if (registrar == null) return;

        Method registerMethod = commandsRegistrarClass.getMethod(
                "register",
                Class.forName("com.mojang.brigadier.tree.LiteralCommandNode"),
                String.class,
                java.util.Collection.class
        );

        for (BaseCommand command : commands) {
            Object node = buildNode(command);
            if (node == null) continue;
            CommandInfo info = command.getCommandInfo();
            registerMethod.invoke(registrar, node, info.description(), Arrays.asList(info.aliases()));
        }
    }

    private Object buildNode(BaseCommand command) {
        CommandInfo info = command.getCommandInfo();
        if (info == null || info.name().isEmpty()) return null;

        try {
            // LiteralArgumentBuilder.literal(name)
            Object builder = literalBuilderClass
                    .getMethod("literal", String.class)
                    .invoke(null, info.name());

            // .requires(Predicate)
            Object requiresPredicate = Proxy.newProxyInstance(
                    plugin.getClass().getClassLoader(),
                    new Class[]{predClass},
                    (proxy, method, args) -> {
                        if ("test".equals(method.getName()) && args != null && args.length == 1) {
                            return checkRequires(args[0], info);
                        }
                        return true;
                    }
            );
            builder = invokeOnBuilder(builder, "requires", new Class[]{predClass}, requiresPredicate);

            // .executes(Command) — no-arg execution
            Object noArgExecutor = makeCommandProxy(command, info, false);
            builder = invokeOnBuilder(builder, "executes", new Class[]{brigadierCommandClass}, noArgExecutor);

            // RequiredArgumentBuilder.argument("args", greedyString())
            Object greedyType  = greedyStringClass.getMethod("greedyString").invoke(null);
            Object argBuilder  = requiredBuilderClass
                    .getMethod("argument", String.class, argumentTypeClass)
                    .invoke(null, "args", greedyType);

            // arg .suggests(SuggestionProvider)
            Object suggestionProvider = Proxy.newProxyInstance(
                    plugin.getClass().getClassLoader(),
                    new Class[]{suggestionProviderClass},
                    (proxy, method, args) -> {
                        if ("getSuggestions".equals(method.getName()) && args != null && args.length == 2) {
                            return buildSuggestions(command, info, args[0], args[1]);
                        }
                        return null;
                    }
            );
            argBuilder = invokeOnBuilder(argBuilder, "suggests", new Class[]{suggestionProviderClass}, suggestionProvider);

            // arg .executes(Command)
            Object argExecutor = makeCommandProxy(command, info, true);
            argBuilder = invokeOnBuilder(argBuilder, "executes", new Class[]{brigadierCommandClass}, argExecutor);

            // build the arg node then attach it
            Object argNode = argBuilder.getClass().getMethod("build").invoke(argBuilder);
            builder = invokeOnBuilder(builder, "then", new Class[]{commandNodeClass}, argNode);

            return builder.getClass().getMethod("build").invoke(builder);

        } catch (Exception e) {
            plugin.getLogger().warning("[WinterCore] Failed to build brigadier node for /" + info.name() + ": " + e.getMessage());
            return null;
        }
    }

    private Object makeCommandProxy(BaseCommand command, CommandInfo info, boolean parseArgs) {
        return Proxy.newProxyInstance(
                plugin.getClass().getClassLoader(),
                new Class[]{brigadierCommandClass},
                (proxy, method, args) -> {
                    if ("run".equals(method.getName()) && args != null && args.length == 1) {
                        Object ctx = args[0];
                        CommandSender sender = getSenderFromContext(ctx);
                        if (sender == null) return 1;

                        String[] parsedArgs = new String[0];
                        if (parseArgs) {
                            String raw = getRawInput(ctx);
                            if (raw != null && raw.contains(" ")) {
                                parsedArgs = raw.substring(raw.indexOf(' ') + 1).split(" ");
                            }
                        }

                        dispatch(command, sender, info.name(), parsedArgs);
                    }
                    return 1;
                }
        );
    }

    private boolean checkRequires(Object source, CommandInfo info) {
        try {
            CommandSender sender = getSenderFromSource(source);
            if (sender == null) return false;
            if (info.inGameOnly() && !(sender instanceof Player)) return false;
            String[] permissions = info.permission();
            if (permissions.length == 0) return true;
            for (String perm : permissions) {
                if (sender.hasPermission(perm)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** source = CommandSourceStack */
    private CommandSender getSenderFromSource(Object source) {
        try {
            return (CommandSender) source.getClass().getMethod("getSender").invoke(source);
        } catch (Exception e) {
            return null;
        }
    }

    /** context = CommandContext<CommandSourceStack> */
    private CommandSender getSenderFromContext(Object context) {
        try {
            Object source = context.getClass().getMethod("getSource").invoke(context);
            return getSenderFromSource(source);
        } catch (Exception e) {
            return null;
        }
    }

    private String getRawInput(Object context) {
        try {
            return (String) context.getClass().getMethod("getInput").invoke(context);
        } catch (Exception e) {
            return null;
        }
    }

    private Object buildSuggestions(BaseCommand command, CommandInfo info, Object context, Object suggestionsBuilder) {
        try {
            CommandSender sender = getSenderFromContext(context);
            if (sender == null) sender = plugin.getServer().getConsoleSender();

            String input = getRawInput(context);
            String argsPart = (input != null && input.contains(" ")) ? input.substring(input.indexOf(' ') + 1) : "";
            String[] current = argsPart.isEmpty() ? new String[0] : argsPart.split(" ", -1);

            List<String> suggestions = command.onTabComplete(
                    new CommandArguments(sender, current, info.name()));

            Method suggestMethod = suggestionsBuilder.getClass().getMethod("suggest", String.class);
            for (String s : suggestions) suggestMethod.invoke(suggestionsBuilder, s);

        } catch (Exception ignored) {}

        try {
            return suggestionsBuilder.getClass().getMethod("buildFuture").invoke(suggestionsBuilder);
        } catch (Exception e) {
            return null;
        }
    }

    private void dispatch(BaseCommand command, CommandSender sender, String label, String[] args) {
        CommandArguments commandArgs = new CommandArguments(sender, args, label);
        if (command.getCommandInfo().async()) {
            plugin.getTasks().async(() -> command.execute(commandArgs));
        } else {
            command.execute(commandArgs);
        }
    }

    /**
     * Walks up the class hierarchy to find and invoke a method by name.
     * Needed because Brigadier builder methods return `this` typed as the concrete subclass.
     */
    private Object invokeOnBuilder(Object target, String methodName, Class<?>[] paramTypes, Object... params) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Method m = cls.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m.invoke(target, params);
            } catch (NoSuchMethodException ignored) {
                cls = cls.getSuperclass();
            }
        }
        // Also try interfaces
        for (Class<?> iface : target.getClass().getInterfaces()) {
            try {
                Method m = iface.getDeclaredMethod(methodName, paramTypes);
                m.setAccessible(true);
                return m.invoke(target, params);
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName);
    }
}