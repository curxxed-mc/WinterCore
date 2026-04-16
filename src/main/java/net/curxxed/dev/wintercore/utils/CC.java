package net.curxxed.dev.wintercore.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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

    public static BaseComponent[] translateToComponent(final String in) {
        return TextComponent.fromLegacyText(translate(in));
    }

    public static String stripColor(final String input) {
        return ChatColor.stripColor(input);
    }
}

