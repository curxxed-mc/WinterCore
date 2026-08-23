package net.curxxed.dev.wintercore.disguise.packet;

import org.bukkit.entity.Player;

public interface DisguisePacketAdapter {
    String name();

    void refresh(Player player, Object entityPlayer, Runnable legacySelfRefresh) throws Exception;
}
