package net.curxxed.dev.wintercore.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class CC {
    public static final String BLACK = ChatColor.BLACK.toString();
    public static final String DARK_BLUE = ChatColor.DARK_BLUE.toString();
    public static final String DARK_GREEN = ChatColor.DARK_GREEN.toString();
    public static final String DARK_AQUA = ChatColor.DARK_AQUA.toString();
    public static final String DARK_RED = ChatColor.DARK_RED.toString();
    public static final String DARK_PURPLE = ChatColor.DARK_PURPLE.toString();
    public static final String GOLD = ChatColor.GOLD.toString();
    public static final String GRAY = ChatColor.GRAY.toString();
    public static final String DARK_GRAY = ChatColor.DARK_GRAY.toString();
    public static final String BLUE = ChatColor.BLUE.toString();
    public static final String GREEN = ChatColor.GREEN.toString();
    public static final String AQUA = ChatColor.AQUA.toString();
    public static final String RED = ChatColor.RED.toString();
    public static final String LIGHT_PURPLE = ChatColor.LIGHT_PURPLE.toString();
    public static final String YELLOW = ChatColor.YELLOW.toString();
    public static final String WHITE = ChatColor.WHITE.toString();
    public static final String MAGIC = ChatColor.MAGIC.toString();
    public static final String BOLD = ChatColor.BOLD.toString();
    public static final String STRIKETHROUGH = ChatColor.STRIKETHROUGH.toString();
    public static final String UNDERLINE = ChatColor.UNDERLINE.toString();
    public static final String ITALIC = ChatColor.ITALIC.toString();
    public static final String RESET = ChatColor.RESET.toString();

    public static String translate(final String in) {
        return ChatColor.translateAlternateColorCodes('&', in);
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        return ChatColor.translateAlternateColorCodes(altColorChar, textToTranslate);
    }

    public static List<String> translate(final List<String> lines) {
        return lines.stream()
                .map(CC::translate)
                .collect(Collectors.toList());
    }

    public static BaseComponent[] translateToComponent(final String in) {
        return TextComponent.fromLegacyText(translate(in));
    }

    public static String stripColor(final String input) {
        return ChatColor.stripColor(input);
    }
}

