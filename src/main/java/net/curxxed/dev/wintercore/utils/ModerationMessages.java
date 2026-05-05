package net.curxxed.dev.wintercore.utils;

import net.curxxed.dev.wintercore.plugin.WinterCore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ModerationMessages {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private ModerationMessages() {
    }

    public static String formatBanAnnouncement(String targetName, String issuer, String reason, Long expiresAt, boolean silent) {
        String path = silent
                ? "moderation.ban.announcement.silent"
                : "moderation.ban.announcement.public";
        String fallback = silent
                ? "&8[&cSilent Ban&8] &f{target} &7was banned by &f{issuer}&7. &8(&f{duration}&8) &7Reason: &f{reason}"
                : "&8[&cBan&8] &f{target} &7was banned by &f{issuer}&7. &8(&f{duration}&8) &7Reason: &f{reason}";
        return message(path, fallback,
                "{target}", safe(targetName),
                "{issuer}", safe(issuer),
                "{reason}", safe(reason),
                "{duration}", formatDuration(expiresAt));
    }

    public static String formatBanKickMessage(String issuer, String reason, Long expiresAt) {
        return message("moderation.ban.kick",
                "&8[&cBan Notice&8]\n"
                        + "&7You have been banned from the network.\n"
                        + "&7By: &f{issuer}\n"
                        + "&7Duration: &f{duration}\n"
                        + "&7Reason: &f{reason}",
                "{issuer}", safe(issuer),
                "{reason}", safe(reason),
                "{duration}", formatDuration(expiresAt));
    }

    public static String formatJoinRestrictionMessage(String reason, Long expiresAt) {
        return message("moderation.ban.join-restriction",
                "&8[&cBan Notice&8]\n"
                        + "&7You are currently banned.\n"
                        + "&7Duration: &f{duration}\n"
                        + "&7Reason: &f{reason}\n"
                        + "&7You may move, but every other action is blocked.",
                "{reason}", safe(reason),
                "{duration}", formatDuration(expiresAt));
    }

    public static String formatRestrictionReminder(String reason, Long expiresAt) {
        return message("moderation.ban.restriction-reminder",
                "&cYou are banned and may only move. &7Duration: &f{duration} &8| &7Reason: &f{reason}",
                "{reason}", safe(reason),
                "{duration}", formatDuration(expiresAt));
    }

    public static String formatReportMessage(String reporter, String reported, String reason, String server) {
        return message("moderation.report.staff-alert",
                "&8[&cReport&8] &f{reporter} &7reported &f{reported} &7for &f{reason} &8[&7{server}&8]",
                "{reporter}", safe(reporter),
                "{reported}", safe(reported),
                "{reason}", safe(reason),
                "{server}", safe(server));
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

    private static String message(String path, String fallback, String... placeholders) {
        WinterCore plugin = WinterCore.getInstance();
        if (plugin != null && plugin.getMessageConfig() != null) {
            return plugin.getMessageConfig().get(path, fallback, placeholders);
        }

        String output = fallback;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            output = output.replace(placeholders[i], placeholders[i + 1] == null ? "" : placeholders[i + 1]);
        }
        return CC.translate(output);
    }
}
