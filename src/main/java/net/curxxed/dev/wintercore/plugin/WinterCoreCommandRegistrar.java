package net.curxxed.dev.wintercore.plugin;

import net.curxxed.dev.wintercore.auth.AuthModule;
import net.curxxed.dev.wintercore.commands.bungee.ServerManagerCommand;
import net.curxxed.dev.wintercore.commands.framework.CommandHandler;
import net.curxxed.dev.wintercore.commands.gamemode.GameModeCommand;
import net.curxxed.dev.wintercore.commands.misc.*;
import net.curxxed.dev.wintercore.commands.social.DiscordCommand;
import net.curxxed.dev.wintercore.commands.social.TagsCommand;
import net.curxxed.dev.wintercore.commands.staff.*;
import net.curxxed.dev.wintercore.commands.troll.TrollCommand;
import net.curxxed.dev.wintercore.commands.utility.*;
import net.curxxed.dev.wintercore.disguise.commands.DisguiseCommand;
import net.curxxed.dev.wintercore.disguise.commands.UnDisguiseCommand;
import net.curxxed.dev.wintercore.rank.RankCommand;

final class WinterCoreCommandRegistrar {

    private final WinterCore plugin;
    private final CommandHandler commandHandler;

    WinterCoreCommandRegistrar(WinterCore plugin) {
        this.plugin = plugin;
        this.commandHandler = plugin.getCommandHandler();
    }

    AuthModule register() {
        commandHandler.register(new FreezeCommand(plugin.getFreezeListener(), plugin));
        commandHandler.register(ThruCommand.class);
        commandHandler.register(Fly.class);
        commandHandler.register(new TrollCommand(plugin));
        commandHandler.register(InvSeeCommand.class);
        commandHandler.register(Feed.class);
        commandHandler.register(ClearChat.class);
        commandHandler.register(new ChatColorCommand(plugin));
        commandHandler.register(new GameModeCommand(plugin, plugin.getStaffModeManager()));
        commandHandler.register(DiscordCommand.class);
        commandHandler.register(Heal.class);
        commandHandler.register(new GrantCommand(plugin));
        commandHandler.register(ManagePermissionCommand.class);
        commandHandler.register(ReloadConfig.class);
        commandHandler.register(new ListCommand(plugin, plugin.getRankManager()));
        commandHandler.register(VanishCommand.class);
        commandHandler.register(new ReportCommand(plugin, plugin.getTagsManager()));
        commandHandler.register(new StaffChatCommand(plugin, plugin.getStaffChatService()));
        commandHandler.register(AboutCommand.class);
        commandHandler.register(MuteCommand.class);
        commandHandler.register(KickCommand.class);
        commandHandler.register(BanCommand.class);
        commandHandler.register(WarningCommand.class);
        commandHandler.register(UnmuteCommand.class);
        commandHandler.register(new HistoryCommand(plugin, plugin.getMenuConfig()));
        commandHandler.register(FixCommand.class);
        commandHandler.register(MoreCommand.class);
        commandHandler.register(EnchantCommand.class);
        commandHandler.register(PingCommand.class);
        commandHandler.register(MessageCommand.class);
        commandHandler.register(SpeedCommand.class);
        commandHandler.register(ClearEffectsCommand.class);
        commandHandler.register(ServerMuteCommand.class);
        ProfileCommand profile = new ProfileCommand(plugin, plugin.getRedisSocials());
        plugin.getServer().getPluginManager().registerEvents(profile, plugin);
        commandHandler.register(profile);

        commandHandler.register(ServerManagerCommand.class);
        commandHandler.register(JumpToPlayer.class);
        commandHandler.register(StaffListCommand.class);
        commandHandler.register(new StaffModeCommand(plugin, plugin.getStaffModeManager()));
        commandHandler.register(new RankCommand(plugin, plugin.getRankManager()));
        commandHandler.register(CheckNMS.class);
        commandHandler.register(SudoCommand.class);
        commandHandler.register(UnbanCommand.class);
        commandHandler.register(new TagsCommand(plugin.getTagsMenu(), plugin));
        commandHandler.register(new DisguiseCommand(plugin.getDisguiseHandler(), plugin));
        commandHandler.register(new UnDisguiseCommand(plugin.getDisguiseHandler(), plugin));
        commandHandler.register(ReplyCommand.class);
        commandHandler.register(AltsCommand.class);
        commandHandler.register(WhoIsDisguisedCommand.class);

        AuthModule authModule = new AuthModule(plugin, plugin.getDatabaseManager().getMongoDatabase());
        authModule.register(commandHandler);
        return authModule;
    }
}
