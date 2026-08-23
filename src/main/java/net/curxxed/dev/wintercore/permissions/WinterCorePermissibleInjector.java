package net.curxxed.dev.wintercore.permissions;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class WinterCorePermissibleInjector {
    public static final Field HUMAN_ENTITY_PERMISSIBLE_FIELD;
    private static final Field PERMISSIBLE_BASE_ATTACHMENTS_FIELD;

    public static void initPlayer(final Player player) throws IllegalAccessException {
        if (HUMAN_ENTITY_PERMISSIBLE_FIELD == null) {
            throw new IllegalStateException("HUMAN_ENTITY_PERMISSIBLE_FIELD is null. Injection failed.");
        }

        final WinterCorePermissible permissible = new WinterCorePermissible(WinterCore.getInstance(), player);
        HUMAN_ENTITY_PERMISSIBLE_FIELD.set(player, permissible);
    }


    static {
        Field humanEntityPermissibleField = null;
        Field permissibleBaseAttachmentsField;
        try {
            Class<?> clazz = Utilities.getCraftBukkitClass("entity.CraftHumanEntity");
            humanEntityPermissibleField = clazz.getDeclaredField("perm");
            humanEntityPermissibleField.setAccessible(true);
            Bukkit.getLogger().info("[WinterCore] Custom permissible injection is available");
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "[WinterCore] Failed to resolve the permissible field", e);
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
