package net.curxxed.dev.wintercore.database.redis.handler;

import net.curxxed.dev.wintercore.database.redis.packet.packets.*;

public interface RedisPacketHandler {
    void handle(RankTagSyncPacket packet);
    void handle(ServerSwitchPacket packet);
    void handle(ServerStatusPacket packet);
    void handle(StaffActivityPacket packet);
    void handle(RemoteCommandPacket packet);
    void handle(ModerationActionPacket packet);
    void handle(PlayerReportPacket packet);
    void handle(DisguiseStatePacket packet);
    void handle(ConfigSyncPacket packet);
    void handle(ChatBroadcastPacket packet);
    void handle(VanishPacket packet);
    void handle(PlayerUpdatePacket packet);
}
