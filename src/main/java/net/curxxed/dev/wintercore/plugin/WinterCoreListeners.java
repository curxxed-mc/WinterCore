package net.curxxed.dev.wintercore.plugin;

import net.curxxed.dev.wintercore.chat.ChatFilterService;
import net.curxxed.dev.wintercore.chat.ChatListener;
import net.curxxed.dev.wintercore.chat.MessagingService;
import net.curxxed.dev.wintercore.chat.StaffChatService;
import net.curxxed.dev.wintercore.listeners.FreezeListener;
import net.curxxed.dev.wintercore.player.BanList;
import net.curxxed.dev.wintercore.player.PlayerService;

final class WinterCoreListeners {

    private final PlayerService playerService;
    private final MessagingService messagingService;
    private final StaffChatService staffChatService;
    private final ChatListener chatListener;
    private final FreezeListener freezeListener;
    private final BanList banList;

    WinterCoreListeners(
            PlayerService playerService,
            MessagingService messagingService,
            StaffChatService staffChatService,
            ChatListener chatListener,
            FreezeListener freezeListener,
            BanList banList
    ) {
        this.playerService = playerService;
        this.messagingService = messagingService;
        this.staffChatService = staffChatService;
        this.chatListener = chatListener;
        this.freezeListener = freezeListener;
        this.banList = banList;
    }

    PlayerService getPlayerService() {
        return playerService;
    }

    MessagingService getMessagingService() {
        return messagingService;
    }

    StaffChatService getStaffChatService() {
        return staffChatService;
    }

    ChatListener getChatListener() {
        return chatListener;
    }

    FreezeListener getFreezeListener() {
        return freezeListener;
    }

    BanList getBanList() {
        return banList;
    }
}
