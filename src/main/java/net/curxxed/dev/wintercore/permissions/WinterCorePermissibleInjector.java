package net.curxxed.dev.wintercore.permissions;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;

public class WinterCorePermissibleInjector {
    public static final Field HUMAN_ENTITY_PERMISSIBLE_FIELD;
    private static final Field PERMISSIBLE_BASE_ATTACHMENTS_FIELD;

    public static void initPlayer(final Player player) throws IllegalAccessException {
        if (HUMAN_ENTITY_PERMISSIBLE_FIELD == null) {
            throw new IllegalStateException("HUMAN_ENTITY_PERMISSIBLE_FIELD is null. Injection failed.");
        }

        final WinterCorePermissible permissible = new WinterCorePermissible(WinterCore.INSTANCE, player);
        HUMAN_ENTITY_PERMISSIBLE_FIELD.set(player, permissible);
    }


    static {
        Field humanEntityPermissibleField = null;
        Field permissibleBaseAttachmentsField = null;
        try {
            String version = Utilities.getServerVersion();
            String className = "org.bukkit.craftbukkit." + version + ".entity.CraftHumanEntity";
            Class<?> clazz = Class.forName(className);
            humanEntityPermissibleField = clazz.getDeclaredField("perm");
            humanEntityPermissibleField.setAccessible(true);
            System.out.println("[WinterCore] Custom Permissible injected successfully!");
        } catch (Exception e) {
            System.err.println("[WinterCore] Failed to find perm field: " + e.getMessage());
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