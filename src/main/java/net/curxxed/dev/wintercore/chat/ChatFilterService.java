package net.curxxed.dev.wintercore.chat;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ChatFilterService {

    private static final String FILE_NAME = "chat-filter.yml";

    private final WinterCore plugin;
    private FileConfiguration config;
    private boolean enabled;
    private boolean notifyStaff;
    private boolean logToConsole;
    private boolean cancelMessage;
    private boolean normalizeLeetspeak;
    private boolean collapseRepeats;
    private boolean stripSymbols;
    private int maxEditDistance;
    private int minFuzzyLength;
    private String bypassPermission;
    private String staffPermission;
    private final List<FilterCategory> categories = new ArrayList<>();

    public ChatFilterService(WinterCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        this.config = YamlConfiguration.loadConfiguration(file);
        this.enabled = config.getBoolean("enabled", true);
        this.notifyStaff = config.getBoolean("actions.notify-staff", true);
        this.logToConsole = config.getBoolean("actions.log-to-console", true);
        this.cancelMessage = config.getBoolean("actions.cancel-message", true);
        this.normalizeLeetspeak = config.getBoolean("algorithm.normalize-leetspeak", true);
        this.collapseRepeats = config.getBoolean("algorithm.collapse-repeated-letters", true);
        this.stripSymbols = config.getBoolean("algorithm.strip-symbols", true);
        this.maxEditDistance = Math.max(0, config.getInt("algorithm.max-edit-distance", 1));
        this.minFuzzyLength = Math.max(3, config.getInt("algorithm.min-fuzzy-length", 5));
        this.bypassPermission = config.getString("permissions.bypass", "wintercore.chatfilter.bypass");
        this.staffPermission = config.getString("permissions.staff-alert", "wintercore.staff");

        this.categories.clear();
        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection categorySection = section.getConfigurationSection(key);
            if (categorySection == null || !categorySection.getBoolean("enabled", true)) {
                continue;
            }

            String label = categorySection.getString("label", key);
            int sensitivity = Math.max(0, categorySection.getInt("sensitivity", maxEditDistance));
            List<String> triggers = new ArrayList<>();
            triggers.addAll(categorySection.getStringList("triggers"));
            triggers.addAll(categorySection.getStringList("phrases"));
            categories.add(new FilterCategory(key, label, sensitivity, triggers));
        }
    }

    public boolean checkAndNotify(Player sender, String message, MessageChannel channel) {
        FilterResult result = inspect(sender, message, channel);
        if (!result.isBlocked()) {
            return false;
        }

        sender.sendMessage(format("messages.blocked-sender",
                "&cYour message was blocked by the chat filter. &7Category: &f{category}",
                result, sender, message, channel));

        if (notifyStaff) {
            String staffMessage = format("messages.staff-alert",
                    "&8[&cFilter&8] &f{player} &7triggered &f{category}&7 in &f{channel}&7: &f{message}",
                    result, sender, message, channel);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(sender)) {
                    continue;
                }
                if (online.isOp() || hasPermission(online, staffPermission)) {
                    online.sendMessage(staffMessage);
                }
            }
        }

        if (logToConsole) {
            plugin.getLogger().warning("[ChatFilter] " + sender.getName()
                    + " triggered " + result.getCategoryKey()
                    + " in " + channel.getDisplayName()
                    + " using detector '" + result.getDetector() + "'. Message: " + message);
        }

        return cancelMessage;
    }

    public FilterResult inspect(Player sender, String message, MessageChannel channel) {
        if (!enabled || message == null || message.trim().isEmpty()) {
            return FilterResult.allowed();
        }
        if (sender != null && hasPermission(sender, bypassPermission)) {
            return FilterResult.allowed();
        }

        NormalizedMessage normalized = normalizeMessage(message);
        if (normalized.compact.isEmpty()) {
            return FilterResult.allowed();
        }

        for (FilterCategory category : categories) {
            for (String trigger : category.getTriggers()) {
                FilterResult result = matchTrigger(category, trigger, normalized);
                if (result.isBlocked()) {
                    return result;
                }
            }
        }

        return FilterResult.allowed();
    }

    private FilterResult matchTrigger(FilterCategory category, String rawTrigger, NormalizedMessage message) {
        if (rawTrigger == null || rawTrigger.trim().isEmpty()) {
            return FilterResult.allowed();
        }

        NormalizedMessage trigger = normalizeMessage(rawTrigger);
        if (trigger.compact.isEmpty()) {
            return FilterResult.allowed();
        }

        if (message.compact.contains(trigger.compact)) {
            return FilterResult.blocked(category, rawTrigger, "compact");
        }

        if (message.collapsedCompact.contains(trigger.collapsedCompact)) {
            return FilterResult.blocked(category, rawTrigger, "repeat-collapse");
        }

        if (message.visibleWords.contains(trigger.visibleWords)) {
            return FilterResult.blocked(category, rawTrigger, "visible");
        }

        int distance = Math.max(maxEditDistance, category.getSensitivity());
        if (trigger.compact.length() >= minFuzzyLength && distance > 0) {
            if (containsFuzzy(message.compact, trigger.compact, distance)
                    || containsFuzzy(message.collapsedCompact, trigger.collapsedCompact, distance)) {
                return FilterResult.blocked(category, rawTrigger, "fuzzy");
            }
        }

        return FilterResult.allowed();
    }

    private NormalizedMessage normalizeMessage(String input) {
        String lower = input.toLowerCase(Locale.ENGLISH);
        String noColor = CC.stripColor(CC.translate(lower));
        if (noColor == null) {
            noColor = lower;
        }

        String deAccented = Normalizer.normalize(noColor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        StringBuilder visible = new StringBuilder();
        StringBuilder compact = new StringBuilder();

        for (int i = 0; i < deAccented.length(); i++) {
            char mapped = mapCharacter(deAccented.charAt(i));
            if (Character.isLetterOrDigit(mapped)) {
                visible.append(mapped);
                compact.append(mapped);
            } else if (!stripSymbols) {
                visible.append(mapped);
            } else {
                visible.append(' ');
            }
        }

        String visibleWords = visible.toString().replaceAll("\\s+", " ").trim();
        String compactText = compact.toString();
        String collapsed = collapseRepeats ? collapseRepeatedCharacters(compactText) : compactText;
        return new NormalizedMessage(visibleWords, compactText, collapsed);
    }

    private char mapCharacter(char value) {
        if (!normalizeLeetspeak) {
            return value;
        }

        switch (value) {
            case '0':
                return 'o';
            case '1':
            case '!':
            case '|':
                return 'i';
            case '2':
                return 'z';
            case '3':
                return 'e';
            case '4':
            case '@':
                return 'a';
            case '5':
            case '$':
                return 's';
            case '6':
                return 'g';
            case '7':
            case '+':
                return 't';
            case '8':
                return 'b';
            case '9':
                return 'g';
            default:
                return value;
        }
    }

    private String collapseRepeatedCharacters(String input) {
        if (input.isEmpty()) {
            return input;
        }

        StringBuilder output = new StringBuilder();
        char last = 0;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current != last) {
                output.append(current);
                last = current;
            }
        }
        return output.toString();
    }

    private boolean containsFuzzy(String haystack, String needle, int maxDistance) {
        if (haystack.length() < needle.length() - maxDistance) {
            return false;
        }

        int minWindow = Math.max(1, needle.length() - maxDistance);
        int maxWindow = Math.min(haystack.length(), needle.length() + maxDistance);

        for (int size = minWindow; size <= maxWindow; size++) {
            for (int start = 0; start + size <= haystack.length(); start++) {
                String window = haystack.substring(start, start + size);
                if (boundedLevenshtein(window, needle, maxDistance) <= maxDistance) {
                    return true;
                }
            }
        }

        return false;
    }

    private int boundedLevenshtein(String left, String right, int maxDistance) {
        int lengthDifference = Math.abs(left.length() - right.length());
        if (lengthDifference > maxDistance) {
            return maxDistance + 1;
        }

        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            int rowBest = current[0];

            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
                rowBest = Math.min(rowBest, current[j]);
            }

            if (rowBest > maxDistance) {
                return maxDistance + 1;
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[right.length()];
    }

    private String format(String path, String fallback, FilterResult result, Player sender, String message, MessageChannel channel) {
        String template = config.getString(path, fallback);
        return CC.translate(template
                .replace("{player}", sender == null ? "Unknown" : sender.getName())
                .replace("{category}", result.getCategoryLabel())
                .replace("{category_key}", result.getCategoryKey())
                .replace("{trigger}", result.getTrigger())
                .replace("{detector}", result.getDetector())
                .replace("{channel}", channel.getDisplayName())
                .replace("{message}", message == null ? "" : message));
    }

    private boolean hasPermission(Player player, String permission) {
        return permission != null && !permission.trim().isEmpty() && player.hasPermission(permission);
    }

    public enum MessageChannel {
        PUBLIC_CHAT("public chat"),
        STAFF_CHAT("staff chat"),
        PRIVATE_MESSAGE("private message");

        private final String displayName;

        MessageChannel(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final class FilterResult {
        private final boolean blocked;
        private final String categoryKey;
        private final String categoryLabel;
        private final String trigger;
        private final String detector;

        private FilterResult(boolean blocked, String categoryKey, String categoryLabel, String trigger, String detector) {
            this.blocked = blocked;
            this.categoryKey = categoryKey;
            this.categoryLabel = categoryLabel;
            this.trigger = trigger;
            this.detector = detector;
        }

        private static FilterResult allowed() {
            return new FilterResult(false, "", "", "", "");
        }

        private static FilterResult blocked(FilterCategory category, String trigger, String detector) {
            return new FilterResult(true, category.getKey(), category.getLabel(), trigger, detector);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getCategoryKey() {
            return categoryKey;
        }

        public String getCategoryLabel() {
            return categoryLabel;
        }

        public String getTrigger() {
            return trigger;
        }

        public String getDetector() {
            return detector;
        }
    }

    private static final class NormalizedMessage {
        private final String visibleWords;
        private final String compact;
        private final String collapsedCompact;

        private NormalizedMessage(String visibleWords, String compact, String collapsedCompact) {
            this.visibleWords = visibleWords;
            this.compact = compact;
            this.collapsedCompact = collapsedCompact;
        }
    }

    private static final class FilterCategory {
        private final String key;
        private final String label;
        private final int sensitivity;
        private final List<String> triggers;

        private FilterCategory(String key, String label, int sensitivity, List<String> triggers) {
            this.key = key;
            this.label = label;
            this.sensitivity = sensitivity;
            this.triggers = triggers == null ? Collections.emptyList() : triggers;
        }

        private String getKey() {
            return key;
        }

        private String getLabel() {
            return label;
        }

        private int getSensitivity() {
            return sensitivity;
        }

        private List<String> getTriggers() {
            return triggers;
        }
    }
}
