package net.curxxed.dev.icore.permissions;

import net.curxxed.dev.icore.plugin.iCore;
import net.curxxed.dev.icore.utils.NMSUtils;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;

public class iCorePermissibleInjector {
    public static final Field HUMAN_ENTITY_PERMISSIBLE_FIELD;
    private static final Field PERMISSIBLE_BASE_ATTACHMENTS_FIELD;

    public static void initPlayer(final Player player) throws IllegalAccessException {
        if (HUMAN_ENTITY_PERMISSIBLE_FIELD == null) {
            throw new IllegalStateException("HUMAN_ENTITY_PERMISSIBLE_FIELD is null. Injection failed.");
        }

        final iCorePermissible permissible = new iCorePermissible(iCore.INSTANCE, player);
        HUMAN_ENTITY_PERMISSIBLE_FIELD.set(player, permissible);
    }


    static {
        Field humanEntityPermissibleField = null;
        Field permissibleBaseAttachmentsField = null;
        try {
            String version = NMSUtils.getServerVersion();
            String className = "org.bukkit.craftbukkit." + version + ".entity.CraftHumanEntity";
            Class<?> clazz = Class.forName(className);
            humanEntityPermissibleField = clazz.getDeclaredField("perm");
            humanEntityPermissibleField.setAccessible(true);
            System.out.println("[iCore] Custom Permissible injected successfully!");
        } catch (Exception e) {
            System.err.println("[iCore] Failed to find perm field: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            permissibleBaseAttachmentsField = PermissibleBase.class.getDeclaredField("attachments");
            permissibleBaseAttachmentsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
        HUMAN_ENTITY_PERMISSIBLE_FIELD = humanEntityPermissibleField;
        PERMISSIBLE_BASE_ATTACHMENTS_FIELD = permissibleBaseAttachmentsField;
    }
}