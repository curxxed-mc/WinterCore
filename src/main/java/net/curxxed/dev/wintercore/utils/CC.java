package net.curxxed.dev.wintercore.utils;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class CC {

    public static String translate(final String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    public static List<String> translate(final List<String> lines) {
        return lines.stream()
                .map(CC::translate)
                .collect(Collectors.toList());
    }

    public static String stripColor(final String input) {
        return ChatColor.stripColor(input);
    }
}

