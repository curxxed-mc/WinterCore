package net.curxxed.dev.wintercore.disguise.packet;

import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.plugin.WinterCore;
import net.curxxed.dev.wintercore.utils.Utilities;

public final class DisguisePacketAdapters {

    private DisguisePacketAdapters() {
    }

    public static DisguisePacketAdapter create(WinterCore plugin, DisguiseHandler handler) {
        if (Utilities.IS_LEGACY) {
            return new LegacyDisguisePacketAdapter(plugin, handler);
        }
        return new ModernDisguisePacketAdapter(plugin);
    }
}
