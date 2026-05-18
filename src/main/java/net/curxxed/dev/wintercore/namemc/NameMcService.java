package net.curxxed.dev.wintercore.namemc;

import net.curxxed.dev.wintercore.plugin.WinterCore;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

public final class NameMcService {

    private final WinterCore plugin;
    private final OkHttpClient client = new OkHttpClient();

    public NameMcService(WinterCore plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("NameMC.enabled", false);
    }

    public void hasLikedConfiguredServer(UUID uuid, Consumer<Boolean> callback) {
        hasLikedServer(uuid, configuredServer(), callback);
    }

    public void hasLikedServer(UUID uuid, String server, Consumer<Boolean> callback) {
        if (uuid == null || server == null || server.trim().isEmpty()) {
            complete(callback, false);
            return;
        }

        plugin.getTasks().async(() -> {
            boolean liked = false;
            try {
                liked = hasLikedServerSync(uuid, server.trim());
            } catch (Exception e) {
                plugin.getLogger().warning("NameMC like check failed for " + uuid + ": " + e.getMessage());
            }
            complete(callback, liked);
        });
    }

    public boolean hasLikedServerSync(UUID uuid, String server) throws IOException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.namemc.com")
                .addPathSegment("server")
                .addPathSegment(server)
                .addPathSegment("likes")
                .addQueryParameter("profile", uuid.toString())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }

            ResponseBody body = response.body();
            if (body == null) {
                return false;
            }

            return Boolean.parseBoolean(body.string().trim());
        }
    }

    private String configuredServer() {
        String configured = plugin.getConfig().getString("NameMC.server", "");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return plugin.getConfig().getString("server-name", "");
    }

    private void complete(Consumer<Boolean> callback, boolean value) {
        if (callback != null) {
            plugin.getTasks().sync(() -> callback.accept(value));
        }
    }
}
