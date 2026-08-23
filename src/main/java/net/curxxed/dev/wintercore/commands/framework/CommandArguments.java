package net.curxxed.dev.wintercore.commands.framework;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class CommandArguments {

    private final CommandSender sender;
    private final String[] args;
    private final String label;

    public CommandArguments(CommandSender sender, String[] args, String label) {
        this.sender = sender;
        this.args = args == null ? new String[0] : args.clone();
        this.label = label;
    }

    public Player getPlayer() {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    public boolean isPlayer() {
        return this.sender instanceof Player;
    }

    public int length() {
        return args.length;
    }

    public String[] getArgs() {
        return args.clone();
    }

    public Optional<String> getOptionalString(int index) {
        if (index >= 0 && index < args.length) {
            return Optional.of(args[index]);
        }
        return Optional.empty();
    }

    public Optional<Player> getOptionalPlayer(int index) {
        return getOptionalString(index).map(Bukkit::getPlayer);
    }

    public Optional<Integer> getOptionalInteger(int index) {
        return getOptionalString(index).flatMap(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    public List<String> getArgsList() {
        return Collections.unmodifiableList(Arrays.asList(args.clone()));
    }
}

