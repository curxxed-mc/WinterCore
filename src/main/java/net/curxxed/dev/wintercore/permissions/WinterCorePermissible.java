package net.curxxed.dev.wintercore.permissions;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;

public class WinterCorePermissible extends PermissibleBase {
    private final WinterCore plugin;
    private final Player player;
    private final Map<String, Boolean> rawPermissions = new HashMap<>();
    private ServerOperator opable;

    private static final Field ATTACHMENTS_FIELD;

    static {
        try {
            ATTACHMENTS_FIELD = PermissibleBase.class.getDeclaredField("attachments");
            ATTACHMENTS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Map<String, PermissionAttachmentInfo> permissions = new HashMap<>();
    private final List<PermissionAttachment> attachments = new ArrayList<>();

    /**
     * Constructor: O(1) excluding recalculatePermissions().
     * recalculatePermissions() is O(P + R + D + C), where:
     * P = currently tracked effective permissions,
     * R = raw permission entries,
     * D = default permissions,
     * C = recursive child-permission traversal.
     */
    public WinterCorePermissible(WinterCore plugin, Player player) {
        super((ServerOperator) player);
        this.plugin = plugin;
        this.player = player;
        this.opable = (ServerOperator) player;
        try {
            ATTACHMENTS_FIELD.set(this, attachments);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set attachments field", e);
        }
        recalculatePermissions();
    }

    /** isOp(): O(1) */
    public boolean isOp() {
        return this.opable.isOp();
    }

    /** setOp(boolean): O(1) */
    public void setOp(boolean value) {
        this.opable.setOp(value);
    }

    /** addRawPermission(String, boolean): O(m) average, where m is node length. */
    public void addRawPermission(String node, boolean value) {
        String lower = node.toLowerCase(Locale.ENGLISH);
        rawPermissions.put(lower, value);
    }

    /** removeRawPermission(String): O(m) average. */
    public void removeRawPermission(String node) {
        if (node == null) {
            return;
        }
        String lower = node.toLowerCase(Locale.ENGLISH);
        rawPermissions.remove(lower);
        if (lower.startsWith("-")) {
            rawPermissions.remove(lower.substring(1));
        } else {
            rawPermissions.remove("-" + lower);
        }
    }

    /** clearRawPermissions(): O(R), where R is the number of raw permissions. */
    public void clearRawPermissions() {
        rawPermissions.clear();
    }

    /**
     * isPermissionSet(String): O(m + k) average.
     * m = permission string length,
     * k = number of dot-separated segments checked.
     * Hash lookups are O(1) average; Bukkit lookup is external/implementation-dependent.
     */
    @Override
    public boolean isPermissionSet(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ENGLISH);

        if (rawPermissions.containsKey("-" + lower)) return true;

        if (lower.contains(".")) {
            String[] parts = lower.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(".");
                sb.append(parts[i]);
                String negWildcard = "-" + sb + ".*";
                if (rawPermissions.containsKey(negWildcard)) return true;
            }
        }

        if (rawPermissions.containsKey("*")) return true;

        if (lower.contains(".")) {
            String[] parts = lower.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(".");
                sb.append(parts[i]);
                String wildcard = sb + ".*";
                if (rawPermissions.containsKey(wildcard)) return true;
            }
        }

        if (rawPermissions.containsKey(lower)) return true;

        Permission perm = Bukkit.getServer().getPluginManager().getPermission(name);
        return perm != null;
    }

    /**
     * hasPermission(String): O(m + k) average.
     * m = permission string length,
     * k = number of dot-separated segments checked.
     * External Bukkit permission lookup/default evaluation is implementation-dependent.
     */
    @Override
    public boolean hasPermission(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        String lower = name.toLowerCase(Locale.ENGLISH);

        if (rawPermissions.containsKey("-" + lower)) return false;

        if (lower.contains(".")) {
            String[] parts = lower.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(".");
                sb.append(parts[i]);
                String negWildcard = "-" + sb + ".*";
                if (rawPermissions.containsKey(negWildcard)) return false;
            }
        }

        if (rawPermissions.containsKey("*")) return true;

        if (lower.contains(".")) {
            String[] parts = lower.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(".");
                sb.append(parts[i]);
                String wildcard = sb + ".*";
                if (rawPermissions.containsKey(wildcard) && rawPermissions.get(wildcard)) return true;
            }
        }

        if (rawPermissions.containsKey(lower)) return rawPermissions.get(lower);

        Permission perm = Bukkit.getServer().getPluginManager().getPermission(name);
        if (perm != null) return perm.getDefault().getValue(isOp());
        return Permission.DEFAULT_PERMISSION.getValue(isOp());
    }

    /** isPermissionSet(Permission): O(m), delegates to isPermissionSet(String). */
    @Override
    public boolean isPermissionSet(Permission perm) {
        if (perm == null) throw new IllegalArgumentException("Permission cannot be null");
        return isPermissionSet(perm.getName());
    }

    /** hasPermission(Permission): O(m), delegates to hasPermission(String). */
    @Override
    public boolean hasPermission(Permission perm) {
        if (perm == null) throw new IllegalArgumentException("Permission cannot be null");
        return hasPermission(perm.getName());
    }

    /**
     * addAttachment(Plugin, String, boolean): O(A + P) amortized + recalculatePermissions().
     * A = attachments size for addAttachment(plugin) and P = permission rebuild cost.
     */
    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        if (WinterCore.isShuttingDown) return null;
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (!plugin.isEnabled()) throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is disabled");

        PermissionAttachment result = addAttachment(plugin);
        result.setPermission(name, value);
        recalculatePermissions();
        return result;
    }

    /**
     * addAttachment(Plugin): amortized O(1) + recalculatePermissions().
     * The expensive part is the permission rebuild.
     */
    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        if (WinterCore.isShuttingDown) return null;
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (!plugin.isEnabled()) throw new IllegalArgumentException("Plugin " + plugin.getDescription().getFullName() + " is disabled");

        PermissionAttachment result = new PermissionAttachment(plugin, this.player);
        attachments.add(result);
        recalculatePermissions();
        return result;
    }

    /**
     * removeAttachment(PermissionAttachment): O(A) average, due to list contains/remove,
     * plus recalculatePermissions().
     */
    @Override
    public void removeAttachment(PermissionAttachment attachment) {
        if (WinterCore.isShuttingDown) return;
        if (attachment == null) throw new IllegalArgumentException("Attachment cannot be null");

        if (attachments.contains(attachment)) {
            attachments.remove(attachment);
            PermissionRemovedExecutor ex = attachment.getRemovalCallback();
            if (ex != null) ex.attachmentRemoved(attachment);
            recalculatePermissions();
            return;
        }
        throw new IllegalArgumentException("Given attachment is not part of Permissible object " + this.player);
    }

    /**
     * recalculatePermissions(): O(P + R + D + C).
     * P = current effective permissions cleared,
     * R = raw permissions,
     * D = default permissions,
     * C = recursive traversal of child permissions across defaults and attachments.
     */
    @Override
    public void recalculatePermissions() {
        if (player == null) return;

        clearPermissions();

        for (Map.Entry<String, Boolean> entry : rawPermissions.entrySet()) {
            permissions.put(entry.getKey(), new PermissionAttachmentInfo(player, entry.getKey(), null, entry.getValue()));
        }

        Set<Permission> defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(isOp());
        Bukkit.getServer().getPluginManager().subscribeToDefaultPerms(isOp(), this.player);
        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase();
            permissions.putIfAbsent(name, new PermissionAttachmentInfo(player, name, null, true));
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, this.player);
            calculateChildPermissions(perm.getChildren(), false, null);
        }

        for (PermissionAttachment attachment : attachments) {
            calculateChildPermissions(attachment.getPermissions(), false, attachment);
        }
    }

    /**
     * clearPermissions(): O(P), where P is the number of effective permissions currently tracked.
     */
    @Override
    public synchronized void clearPermissions() {
        if (player == null) return;

        Set<String> perms = new HashSet<>(permissions.keySet());
        for (String name : perms) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, this.player);
        }
        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(false, this.player);
        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(true, this.player);
        permissions.clear();
    }

    /**
     * calculateChildPermissions(Map, boolean, PermissionAttachment):
     * O(C) over the traversed child-permission graph.
     * In the worst case, this can revisit large trees depending on permission structure.
     */
    private void calculateChildPermissions(Map<String, Boolean> children, boolean invert, PermissionAttachment attachment) {
        for (Map.Entry<String, Boolean> entry : children.entrySet()) {
            String node = entry.getKey().toLowerCase();
            boolean value = entry.getValue() ^ invert;
            permissions.put(node, new PermissionAttachmentInfo(player, node, attachment, value));
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(node);
            if (perm != null) {
                calculateChildPermissions(perm.getChildren(), !value, attachment);
            }
        }
    }

    /** getEffectivePermissions(): O(P), where P is the number of effective permissions. */
    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return new HashSet<>(permissions.values());
    }
}