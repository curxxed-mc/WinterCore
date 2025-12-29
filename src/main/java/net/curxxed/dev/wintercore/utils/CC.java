package net.curxxed.dev.wintercore.utils;

import org.bukkit.ChatColor;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class for handling chat color translation.
 */
public class CC {

    /**
     * Translates a string using an alternate color code character ('&').
     * @param in The string to translate.
     * @return The translated string.
     */
    public static String translate(final String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    /**
     * Translates a list of strings using an alternate color code character.
     * @param lines The list of strings to translate.
     * @return The translated list of strings.
     */
    public static List<String> translate(final List<String> lines) {
        return lines.stream()
                .map(CC::translate)
                .collect(Collectors.toList());
    }

    /**
     * Strips all color codes from a string.
     * @param input The string to strip color from.
     * @return The stripped string.
     */
    public static String stripColor(final String input) {
        return ChatColor.stripColor(input);
    }
}

