package net.curxxed.dev.wintercore.config;

import net.curxxed.dev.wintercore.permissions.WinterCorePermissible;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PermissionConfigManager {

    private static final String ROOT_KEY = "players";

    private final WinterCore plugin;
    private final Map<UUID, PermissionEntry> entries = new ConcurrentHashMap<>();
    private File file;

    public PermissionConfigManager(WinterCore plugin) {
        this.plugin = plugin;
        load();
    }

    public synchronized void load() {
        ensureFile();
        entries.clear();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection(ROOT_KEY);
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            PermissionEntry entry = new PermissionEntry();
            for (String grant : section.getStringList("grants")) {
                String node = normalizePermission(grant);
                if (!node.isEmpty()) {
                    entry.grants.add(node);
                }
            }
            for (String deny : section.getStringList("denies")) {
                String node = normalizePermission(deny);
                if (!node.isEmpty()) {
                    entry.denies.add(node);
                }
            }

            if (!entry.grants.isEmpty() || !entry.denies.isEmpty()) {
                entries.put(uuid, entry);
            }
        }
    }

    public synchronized void save() {
        ensureFile();

        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection(ROOT_KEY);
        for (Map.Entry<UUID, PermissionEntry> mapEntry : entries.entrySet()) {
            PermissionEntry entry = mapEntry.getValue();
            if (entry.grants.isEmpty() && entry.denies.isEmpty()) {
                continue;
            }

            ConfigurationSection section = root.createSection(mapEntry.getKey().toString());
            section.set("grants", new ArrayList<>(entry.grants));
            section.set("denies", new ArrayList<>(entry.denies));
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save permissions.yml: " + e.getMessage());
        }
    }

    public synchronized void setGranted(UUID uuid, String permissionNode) {
        String node = normalizePermission(permissionNode);
        if (node.isEmpty()) {
            return;
        }

        PermissionEntry entry = entries.computeIfAbsent(uuid, ignored -> new PermissionEntry());
        entry.denies.remove(node);
        entry.grants.add(node);
        cleanupIfEmpty(uuid, entry);
        save();
    }

    public synchronized void setDenied(UUID uuid, String permissionNode) {
        String node = normalizePermission(permissionNode);
        if (node.isEmpty()) {
            return;
        }

        PermissionEntry entry = entries.computeIfAbsent(uuid, ignored -> new PermissionEntry());
        entry.grants.remove(node);
        entry.denies.add(node);
        cleanupIfEmpty(uuid, entry);
        save();
    }

    public synchronized void removeOverride(UUID uuid, String permissionNode) {
        String node = normalizePermission(permissionNode);
        if (node.isEmpty()) {
            return;
        }

        PermissionEntry entry = entries.get(uuid);
        if (entry == null) {
            return;
        }

        entry.grants.remove(node);
        entry.denies.remove(node);
        cleanupIfEmpty(uuid, entry);
        save();
    }

    @Contract("_ -> new")
    @NonNull()
    public synchronized PermissionEntrySnapshot getSnapshot(UUID uuid) {
        PermissionEntry entry = entries.get(uuid);
        if (entry == null) {
            return new PermissionEntrySnapshot(Collections.<String>emptyList(), Collections.<String>emptyList());
        }

        return new PermissionEntrySnapshot(
                new ArrayList<>(entry.grants),
                new ArrayList<>(entry.denies)
        );
    }

    public synchronized void applyOverrides(UUID uuid, WinterCorePermissible permissible) {
        PermissionEntry entry = entries.get(uuid);
        if (entry == null) {
            return;
        }

        for (String grant : entry.grants) {
            permissible.addRawPermission(grant, true);
        }
        for (String deny : entry.denies) {
            permissible.addRawPermission("-" + deny, true);
        }
    }

    private void cleanupIfEmpty(UUID uuid, PermissionEntry entry) {
        if (entry.grants.isEmpty() && entry.denies.isEmpty()) {
            entries.remove(uuid);
        }
    }

    private String normalizePermission(String permissionNode) {
        if (permissionNode == null) {
            return "";
        }

        String normalized = permissionNode.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private void ensureFile() {
        if (file == null) {
            file = new File(plugin.getDataFolder(), "permissions.yml");
        }

        if (file.exists()) {
            return;
        }

        plugin.saveResource("permissions.yml", false);
        if (file.exists()) {
            return;
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create permissions.yml: " + e.getMessage());
        }
    }

    public static final class PermissionEntrySnapshot {
        private final List<String> grants;
        private final List<String> denies;

        private PermissionEntrySnapshot(List<String> grants, List<String> denies) {
            this.grants = grants;
            this.denies = denies;
        }

        public List<String> getGrants() {
            return grants;
        }

        public List<String> getDenies() {
            return denies;
        }
    }

    private static final class PermissionEntry {
        private final Set<String> grants = new LinkedHashSet<>();
        private final Set<String> denies = new LinkedHashSet<>();
    }
}
