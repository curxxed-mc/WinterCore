package net.curxxed.dev.wintercore.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class SkinFetcher {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build();

    public static void fetchSkin(String playerName, BiConsumer<SkinProperty, Exception> callback) {
        Request uuidRequest = new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + playerName)
                .build();

        CLIENT.newCall(uuidRequest).enqueue(new Callback() {
            @Override
            @SuppressWarnings("NullableProblems")
            public void onFailure(Call call, IOException e) {
                callback.accept(null, e);
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        callback.accept(null, new Exception("Player not found: " + playerName));
                        return;
                    }

                    JsonObject uuidObj = new JsonParser()
                            .parse(r.body().string())
                            .getAsJsonObject();
                    String uuid = uuidObj.get("id").getAsString();

                    fetchSession(uuid, playerName, callback);
                }
            }
        });
    }

    private static void fetchSession(String uuid, String playerName, BiConsumer<SkinProperty, Exception> callback) {
        Request sessionRequest = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false")
                .build();

        CLIENT.newCall(sessionRequest).enqueue(new Callback() {
            @Override
            @SuppressWarnings("NullableProblems")
            public void onFailure(Call call, IOException e) {
                callback.accept(null, e);
            }

            @Override
            @SuppressWarnings("NullableProblems")
            public void onResponse(Call call, Response response) throws IOException {
                try (Response r = response) {
                    if (!r.isSuccessful() || r.body() == null) {
                        callback.accept(null, new Exception("Could not fetch skin data for: " + playerName));
                        return;
                    }

                    JsonObject sessionObj = new JsonParser()
                            .parse(r.body().string())
                            .getAsJsonObject();
                    JsonArray properties = sessionObj.getAsJsonArray("properties");
                    JsonObject skinProperty = properties.get(0).getAsJsonObject();

                    String value = skinProperty.get("value").getAsString();
                    String signature = skinProperty.get("signature").getAsString();

                    callback.accept(new SkinProperty(value, signature), null);
                }
            }
        });
    }

    public static class SkinProperty {
        public final String value;
        public final String signature;

        public SkinProperty(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }
}