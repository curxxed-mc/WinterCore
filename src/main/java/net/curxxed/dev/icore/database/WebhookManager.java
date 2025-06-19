package net.curxxed.dev.icore.database;

import okhttp3.*;
import org.bukkit.entity.Player;

import java.io.IOException;

public class WebhookManager {
    public static void sendWebhook(String url, Player player, String message) {
        OkHttpClient client = new OkHttpClient();

        String json = "{\"content\": \"" + message + "\"}";

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {}
            @Override
            public void onFailure(Call call, IOException e) {}
        });
    }
}
