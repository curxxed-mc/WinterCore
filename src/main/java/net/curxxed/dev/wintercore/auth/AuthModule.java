package net.curxxed.dev.wintercore.auth;

import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.curxxed.dev.wintercore.auth.repository.AuthRepository;
import net.curxxed.dev.wintercore.commands.framework.CommandHandler;
import net.curxxed.dev.wintercore.commands.staff.AuthCommand;
import net.curxxed.dev.wintercore.commands.staff.TwoFACommand;
import net.curxxed.dev.wintercore.plugin.WinterCore;

public class AuthModule {

    private final WinterCore plugin;
    private final MongoDatabase database;

    @Getter
    private AuthManager authManager;

    public AuthModule(WinterCore plugin, MongoDatabase database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void register(CommandHandler commandHandler) {
        AuthRepository repository = new AuthRepository(
                database.getCollection("2fa_secrets")
        );

        authManager = new AuthManager(plugin, repository);

        plugin.getServer().getPluginManager().registerEvents(
                new AuthListener(authManager, plugin), plugin
        );

        commandHandler.register(new AuthCommand(plugin, authManager));
        commandHandler.register(new TwoFACommand(plugin, authManager));
    }

}