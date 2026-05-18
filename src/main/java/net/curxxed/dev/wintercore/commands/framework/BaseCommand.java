package net.curxxed.dev.wintercore.commands.framework;

import lombok.Getter;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

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
            sender.sendMessage(msg("general.no-permission",
                    "&cYou do not have permission to execute this command."));
            return true;
        }

        if (commandInfo.inGameOnly() && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(msg("general.in-game-only-command",
                    "&cThis command can only be executed by players."));
            return true;
        }

        final CommandArguments commandArgs = new CommandArguments(sender, args, label);
        if (commandInfo.async()) {
            this.plugin.getTasks().async(() -> this.execute(commandArgs));
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

        CommandArguments commandArgs = new CommandArguments(sender, args, label);
        List<String> custom = onTabComplete(commandArgs);
        if (custom != null) {
            return completeCurrentArg(commandArgs, custom);
        }

        return suggestFromUsage(commandArgs);
    }

    private boolean hasPermission(CommandSender sender, String[] permissions) {
        if (permissions.length == 0) {
            return true;
        }
        return Arrays.stream(permissions).anyMatch(sender::hasPermission);
    }

    protected boolean hasAnyPermission(CommandSender sender, String... permissions) {
        return hasPermission(sender, permissions);
    }

    protected void runSync(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }
        plugin.getTasks().sync( action);
    }

    protected void runAsync(Runnable action) {
        plugin.getTasks().async( action);
    }

    protected String msg(String path, String fallback, String... placeholders) {
        if (plugin != null && plugin.getMessageConfig() != null) {
            return plugin.getMessageConfig().get(path, fallback, placeholders);
        }

        String output = fallback;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return CC.translate(output);
    }

    protected void send(CommandSender sender, String path, String fallback, String... placeholders) {
        sender.sendMessage(msg(path, fallback, placeholders));
    }

    protected List<String> msgList(String path, List<String> fallback, String... placeholders) {
        if (plugin != null && plugin.getMessageConfig() != null) {
            return plugin.getMessageConfig().getList(path, fallback, placeholders);
        }

        return fallback.stream()
                .map(line -> {
                    String output = line;
                    for (int i = 0; i + 1 < placeholders.length; i += 2) {
                        output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
                    }
                    return CC.translate(output);
                })
                .collect(Collectors.toList());
    }

    protected void sendList(CommandSender sender, String path, List<String> fallback, String... placeholders) {
        for (String line : msgList(path, fallback, placeholders)) {
            sender.sendMessage(line);
        }
    }

    protected void sendUsage(CommandSender sender) {
        send(sender, "general.usage", "&cUsage: {usage}", "{usage}", commandInfo.usage());
    }

    protected List<String> completeOnlinePlayers(CommandArguments args) {
        List<String> players = net.curxxed.dev.wintercore.utils.Utilities.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        return completeCurrentArg(args, players);
    }

    protected List<String> completeCurrentArg(CommandArguments args, Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> uniqueCandidates = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            uniqueCandidates.add(candidate);
        }

        if (uniqueCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        String input = args.length() == 0
                ? ""
                : args.getOptionalString(args.length() - 1).orElse("");
        String loweredInput = input.toLowerCase(Locale.ENGLISH);

        return uniqueCandidates.stream()
                .filter(candidate -> loweredInput.isEmpty()
                        || candidate.toLowerCase(Locale.ENGLISH).startsWith(loweredInput))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private List<String> suggestFromUsage(CommandArguments args) {
        String usage = commandInfo.usage();
        if (usage == null || usage.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = Arrays.stream(usage.trim().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());

        if (!tokens.isEmpty() && tokens.get(0).startsWith("/")) {
            tokens.remove(0);
        }

        int argIndex = args.length() == 0 ? 0 : args.length() - 1;
        if (argIndex < 0 || argIndex >= tokens.size()) {
            return Collections.emptyList();
        }

        String usageToken = tokens.get(argIndex);
        if (isFreeFormToken(usageToken)) {
            return Collections.emptyList();
        }

        if (isPlayerToken(usageToken)) {
            return completeOnlinePlayers(args);
        }

        List<String> tokenCandidates = parseCandidatesFromToken(usageToken);
        return completeCurrentArg(args, tokenCandidates);
    }

    private boolean isPlayerToken(String usageToken) {
        String lowered = usageToken.toLowerCase(Locale.ENGLISH);
        return lowered.contains("player");
    }

    private boolean isFreeFormToken(String usageToken) {
        String lowered = usageToken.toLowerCase(Locale.ENGLISH);
        if (lowered.contains("...")) {
            return true;
        }

        String normalized = lowered.replace("<", "")
                .replace(">", "")
                .replace("[", "")
                .replace("]", "");

        if (normalized.contains("|")) {
            return false;
        }

        return normalized.contains("reason")
                || normalized.contains("message")
                || normalized.contains("command")
                || normalized.contains("text")
                || normalized.contains("description")
                || normalized.contains("link")
                || normalized.contains("url");
    }

    private List<String> parseCandidatesFromToken(String usageToken) {
        String normalized = usageToken.trim()
                .replace("<", "")
                .replace(">", "")
                .replace("[", "")
                .replace("]", "");

        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        String[] rawParts = normalized.split("\\|");
        Set<String> candidates = new LinkedHashSet<>();
        for (String rawPart : rawParts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }

            if (part.matches("\\d+-\\d+")) {
                String[] split = part.split("-");
                int min = Integer.parseInt(split[0]);
                int max = Integer.parseInt(split[1]);
                if (max >= min && (max - min) <= 50) {
                    for (int value = min; value <= max; value++) {
                        candidates.add(String.valueOf(value));
                    }
                    continue;
                }
            }

            if (!part.endsWith("...")) {
                candidates.add(part);
            }
        }

        return new ArrayList<>(candidates);
    }

    public abstract void execute(CommandArguments args);

    public List<String> onTabComplete(CommandArguments args) {
        return null;
    }
}
