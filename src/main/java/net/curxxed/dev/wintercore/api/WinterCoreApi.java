package net.curxxed.dev.wintercore.api;

import net.curxxed.dev.wintercore.chat.ChatFilterService;
import net.curxxed.dev.wintercore.config.MessageConfig;
import net.curxxed.dev.wintercore.database.DatabaseManager;
import net.curxxed.dev.wintercore.database.redis.RedisManager;
import net.curxxed.dev.wintercore.database.redis.packet.RedisPacket;
import net.curxxed.dev.wintercore.database.service.CurrencyService;
import net.curxxed.dev.wintercore.disguise.DisguiseHandler;
import net.curxxed.dev.wintercore.disguise.DisguiseRegistry;
import net.curxxed.dev.wintercore.namemc.NameMcService;
import net.curxxed.dev.wintercore.nametags.NameTagColorManager;
import net.curxxed.dev.wintercore.nms.PacketSender;
import net.curxxed.dev.wintercore.player.PlayerService;
import net.curxxed.dev.wintercore.rank.RankManager;
import net.curxxed.dev.wintercore.scheduler.Tasks;
import net.curxxed.dev.wintercore.tags.TagsManager;
import org.bukkit.entity.Player;

import java.util.List;

public interface WinterCoreApi {
    Tasks tasks();

    PacketSender packetSender();


    String serverVersion();

    boolean isAtLeastMinecraft(int minorVersion);

    CurrencyService currency();

    DatabaseManager database();

    RedisManager redis();

    MessageConfig messages();

    ChatFilterService chatFilter();

    RankManager ranks();

    TagsManager tags();

    PlayerService players();

    DisguiseHandler disguises();

    DisguiseRegistry disguiseRegistry();

    NameMcService nameMc();

    NameTagColorManager nametags();

    List<Player> onlinePlayers();

    void publish(RedisPacket<?> packet);
}
