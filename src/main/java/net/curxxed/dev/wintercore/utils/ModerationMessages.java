package net.curxxed.dev.wintercore.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ModerationMessages {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private ModerationMessages() {
    }

    public static String formatBanAnnouncement(String targetName, String issuer, String reason, Long expiresAt, boolean silent) {
        String header = silent ? "&8[&cSilent Ban&8] " : "&8[&cBan&8] ";
        return CC.translate(header
                + "&f" + safe(targetName)
                + " &7was banned by &f" + safe(issuer)
                + "&7. &8(&f" + formatDuration(expiresAt) + "&8) &7Reason: &f" + safe(reason));
    }

    public static String formatBanKickMessage(String issuer, String reason, Long expiresAt) {
        return CC.translate("&8[&cBan Notice&8]\n"
                + "&7You have been banned from the network.\n"
                + "&7By: &f" + safe(issuer) + "\n"
                + "&7Duration: &f" + formatDuration(expiresAt) + "\n"
                + "&7Reason: &f" + safe(reason));
    }

    public static String formatJoinRestrictionMessage(String reason, Long expiresAt) {
        return CC.translate("&8[&cBan Notice&8]\n"
                + "&7You are currently banned.\n"
                + "&7Duration: &f" + formatDuration(expiresAt) + "\n"
                + "&7Reason: &f" + safe(reason) + "\n"
                + "&7You may move, but every other action is blocked.");
    }

    public static String formatRestrictionReminder(String reason, Long expiresAt) {
        return CC.translate("&cYou are banned and may only move. &7Duration: &f"
                + formatDuration(expiresAt) + " &8| &7Reason: &f" + safe(reason));
    }

    public static String formatReportMessage(String reporter, String reported, String reason, String server) {
        return CC.translate("&8[&cReport&8] &f" + safe(reporter)
                + " &7reported &f" + safe(reported)
                + " &7for &f" + safe(reason)
                + " &8[&7" + safe(server) + "&8]");
    }

    public static String formatDuration(Long expiresAt) {
        if (expiresAt == null) {
            return "Permanent";
        }

        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            return "Expired";
        }

        return "Until " + formatTimestamp(expiresAt);
    }

    public static String formatTimestamp(long epochMillis) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
        return format.format(new Date(epochMillis));
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }
        return value;
    }
}
