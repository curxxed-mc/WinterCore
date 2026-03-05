package net.curxxed.dev.wintercore.nametags;

import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Team;

public final class NameTagVersionHelper {

    public static final boolean HAS_TEAM_COLOR_API = probeTeamColorApi();

    private NameTagVersionHelper() {}

    private static boolean probeTeamColorApi() {
        try {
            //noinspection JavaReflectionMemberAccess
            Team.class.getMethod("setColor", ChatColor.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static void applyColor(Team team, String rawColor) {
        if (rawColor == null || rawColor.isEmpty()) rawColor = "&f";

        String translated = CC.translate(rawColor);
        String safePrefix = translated.length() > 16 ? translated.substring(0, 16) : translated;

        team.setPrefix(safePrefix);
        team.setSuffix("");

        if (HAS_TEAM_COLOR_API) {
            ChatColor primary = extractPrimaryColor(translated);
            if (primary != null) {
                try {
                    team.getClass().getMethod("setColor", ChatColor.class).invoke(team, primary);
                } catch (Exception ignored) {}
            }
        }
    }

    public static void resetColor(Team team) {
        team.setPrefix("");
        team.setSuffix("");
        if (HAS_TEAM_COLOR_API) {
            try {
                team.getClass().getMethod("setColor", ChatColor.class).invoke(team, ChatColor.WHITE);
            } catch (Exception ignored) {}
        }
    }

    static ChatColor extractPrimaryColor(String translated) {
        for (int i = 0; i + 1 < translated.length(); i++) {
            if (translated.charAt(i) == ChatColor.COLOR_CHAR) {
                ChatColor c = ChatColor.getByChar(translated.charAt(i + 1));
                if (c != null && isDisplayColor(c)) return c;
            }
        }
        return null;
    }

    private static boolean isDisplayColor(ChatColor c) {
        return c != ChatColor.BOLD
                && c != ChatColor.ITALIC
                && c != ChatColor.UNDERLINE
                && c != ChatColor.STRIKETHROUGH
                && c != ChatColor.MAGIC
                && c != ChatColor.RESET;
    }
}