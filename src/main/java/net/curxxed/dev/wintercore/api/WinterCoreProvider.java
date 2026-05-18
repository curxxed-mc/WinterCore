package net.curxxed.dev.wintercore.api;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class WinterCoreProvider {

    private WinterCoreProvider() {
    }

    public static WinterCoreApi get() {
        RegisteredServiceProvider<WinterCoreApi> registration =
                Bukkit.getServicesManager().getRegistration(WinterCoreApi.class);
        if (registration != null) {
            return registration.getProvider();
        }

        WinterCore plugin = WinterCore.getInstance();
        return plugin != null ? plugin.getApi() : null;
    }
}
