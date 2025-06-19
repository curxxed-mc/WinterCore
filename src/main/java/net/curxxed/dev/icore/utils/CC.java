package net.curxxed.dev.icore.utils;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.List;

public class CC {
    public static String translate(final String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    public static List<String> translate(final List<String> lines) {
        final List<String> toReturn = new ArrayList<String>();
        for (final String line : lines) {
            toReturn.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return toReturn;
    }

    public static String[] translate(final String[] lines) {
        final List<String> toReturn = new ArrayList<String>();
        for (final String line : lines) {
            if (line != null) {
                toReturn.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        return toReturn.toArray(new String[toReturn.size()]);
    }

    public static String stripColor(final String input) {
        return ChatColor.stripColor(input);
    }
}
