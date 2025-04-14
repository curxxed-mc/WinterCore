package curxxed.dev.icore.utils;


import curxxed.dev.icore.Bungee.BungeeListener;
import curxxed.dev.icore.Commands.Gamemode.gma;
import curxxed.dev.icore.Commands.Gamemode.gmc;
import curxxed.dev.icore.Commands.Gamemode.gms;
import curxxed.dev.icore.Commands.Gamemode.gmsp;
import curxxed.dev.icore.Commands.Bungee.ServerManagerCommand;
import curxxed.dev.icore.Commands.Staff.*;
import curxxed.dev.icore.Commands.Utility.*;
import curxxed.dev.icore.Commands.misc.*;
import curxxed.dev.icore.listeners.FreezeListener;
import curxxed.dev.icore.listeners.PlayerListener;
import curxxed.dev.icore.Commands.Troll.TrollCommand;
import curxxed.dev.icore.Commands.Troll.WinCommand;
import curxxed.dev.icore.Main;
import curxxed.dev.icore.utils.GUI.ColorGUI;
import curxxed.dev.icore.utils.GUI.DisguiseGUI;
import curxxed.dev.icore.utils.GUI.RankGUIListener;
import curxxed.dev.icore.utils.GUI.StaffListGUI;
import curxxed.dev.icore.utils.Staff.StaffData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.messaging.Messenger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegisterPlugin {
    private PlayerListener playerListener;
    private FreezeListener freezeListener;
    private SocialInput socialInput;
    private RankManager rankManager;
    private DisguiseGUI disguiseGUI;
    private final Map<UUID, StaffData> staffMap = new HashMap<>();


    public void registerPlugin(Main pl) {
        RankManager.initialize(pl);
        RankManager rankManager = RankManager.getInstance();
        disguiseGUI = new DisguiseGUI(rankManager);


        registerListeners(pl);
        registerCommands(pl);
        registerBungee(pl);
    }

    public void registerBungee(Main pl) {
        Messenger bm = pl.getServer().getMessenger();

        /*
         * Outgoing Plugin Channel
         * This is used to send data to the BungeeCord server.
         *
         */
        bm.registerOutgoingPluginChannel(pl, "BungeeCord");

        /*
         * Incoming Plugin Channel
         * This is used to receive data from the BungeeCord server.
         *
         */
        bm.registerIncomingPluginChannel(pl, "BungeeCord", new BungeeListener(pl));
    }


    public void registerListeners(Main pl) {
        PluginManager pm = pl.getServer().getPluginManager();

        playerListener = new PlayerListener(pl);
        freezeListener = new FreezeListener(playerListener);
        socialInput = new SocialInput(pl);

        // Register Listeners
        pm.registerEvents(playerListener, pl);
        pm.registerEvents(new RankGUIListener(pl), pl);
        pm.registerEvents(freezeListener, pl);
        pm.registerEvents(new ProfileCommand(pl, pl.getRedisManager()), pl);
        pm.registerEvents(socialInput, pl);
        pm.registerEvents(new StaffListGUI(pl), pl);
    }

    public void registerCommands(Main pl) {
        // Ensure dependencies are initialized
        if (freezeListener == null) {
            freezeListener = new FreezeListener(playerListener);
        }

        pl.getCommand("freeze").setExecutor(new FreezeCommand(freezeListener));
        pl.getCommand("thru").setExecutor(new ThruCommand());
        /*pl.getCommand("alts").setExecutor(new AltsCommand(altManager));*/
        pl.getCommand("fly").setExecutor(new Fly());
        pl.getCommand("troll").setExecutor(new TrollCommand());
        pl.getCommand("invsee").setExecutor(new InvSeeCommand());
        pl.getCommand("feed").setExecutor(new Feed());
        pl.getCommand("clearchat").setExecutor(new ClearChat());
        pl.getCommand("chatcolor").setExecutor(new ColorGUI(pl));
        pl.getCommand("gmc").setExecutor(new gmc());
        pl.getCommand("gma").setExecutor(new gma());
        pl.getCommand("gms").setExecutor(new gms());
        pl.getCommand("gmsp").setExecutor(new gmsp());
        pl.getCommand("heal").setExecutor(new Heal());
        pl.getCommand("setrank").setExecutor(new SetRankCommand(pl));
        pl.getCommand("permission").setExecutor(new ManagePermissionCommand(pl));
        pl.getCommand("reloadconfig").setExecutor(new ReloadConfig(pl));
        pl.getCommand("list").setExecutor(new ListCommand(pl, RankManager.getInstance()));
        pl.getCommand("vanish").setExecutor(new VanishCommand(pl));
        pl.getCommand("report").setExecutor(new ReportCommand(pl));
        pl.getCommand("sc").setExecutor(new StaffChatCommand(pl, playerListener));
        pl.getCommand("ac").setExecutor(new AdminChatCommand(pl, playerListener));
        pl.getCommand("mc").setExecutor(new ManagerChatCommand(pl, playerListener));
        pl.getCommand("about").setExecutor(new AboutCommand(pl));
        pl.getCommand("mute").setExecutor(new MuteCommand(pl));
        pl.getCommand("kick").setExecutor(new KickCommand(pl));
        pl.getCommand("ban").setExecutor(new BanCommand(pl));
        pl.getCommand("tban").setExecutor(new TempBanCommand(pl));
        pl.getCommand("warn").setExecutor(new WarnCommand(pl));
        pl.getCommand("unmute").setExecutor(new UnmuteCommand(pl));
        pl.getCommand("history").setExecutor(new HistoryCommand(pl));
        pl.getCommand("fix").setExecutor(new FixCommand(pl));
        pl.getCommand("more").setExecutor(new MoreCommand(pl));
        pl.getCommand("enchant").setExecutor(new EnchantCommand(pl));
        pl.getCommand("ping").setExecutor(new PingCommand());
        pl.getCommand("message").setExecutor(new MessageCommand(pl));
        pl.getCommand("speed").setExecutor(new SpeedCommand());
        pl.getCommand("cleareffects").setExecutor(new ClearEffectsCommand());
        pl.getCommand("win").setExecutor(new WinCommand());
        pl.getCommand("profile").setExecutor(new ProfileCommand(pl, pl.getRedisManager()));
        pl.getCommand("tps").setExecutor(new TPSCommand());
        pl.getCommand("servermanager").setExecutor(new ServerManagerCommand(pl));
        pl.getCommand("jtp").setExecutor(new JumpToPlayer());
        pl.getCommand("stafflist").setExecutor(new StaffListCommand(pl));
        /*pl.getCommand("disguise").setExecutor(new DisguiseCommand(pl, disguiseGUI, RankManager.getInstance()));*/
    }
}
