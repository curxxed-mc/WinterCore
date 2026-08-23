package net.curxxed.dev.wintercore.utils;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern TOKEN = Pattern.compile("^(\\d+)([smhdw])$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
        throw new UnsupportedOperationException();
    }

    public static Duration parse(String token) {
        if (token == null) {
            return null;
        }

        Matcher matcher = TOKEN.matcher(token.trim());
        if (!matcher.matches()) {
            return null;
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }

        if (amount <= 0L) {
            return null;
        }

        String unit = matcher.group(2).toLowerCase(Locale.ENGLISH);
        try {
            switch (unit) {
                case "s":
                    return Duration.ofSeconds(amount);
                case "m":
                    return Duration.ofMinutes(amount);
                case "h":
                    return Duration.ofHours(amount);
                case "d":
                    return Duration.ofDays(amount);
                case "w":
                    return Duration.ofDays(Math.multiplyExact(amount, 7L));
                default:
                    return null;
            }
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    public static boolean isDurationToken(String token) {
        return parse(token) != null;
    }
}
