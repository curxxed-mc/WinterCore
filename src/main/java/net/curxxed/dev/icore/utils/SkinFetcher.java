package net.curxxed.dev.icore.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinFetcher {

    public static SkinProperty fetchSkin(String playerName) throws Exception {
        URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
        HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
        uuidConn.setRequestMethod("GET");
        uuidConn.setConnectTimeout(3000);
        uuidConn.setReadTimeout(3000);

        if (uuidConn.getResponseCode() != 200) {
            throw new Exception("Player not found: " + playerName);
        }

        BufferedReader uuidReader = new BufferedReader(new InputStreamReader(uuidConn.getInputStream()));
        JsonObject uuidObj = new JsonParser().parse(uuidReader).getAsJsonObject();
        String uuid = uuidObj.get("id").getAsString();

        // Step 2: Get skin properties from session server
        URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        HttpURLConnection sessionConn = (HttpURLConnection) sessionUrl.openConnection();
        sessionConn.setRequestMethod("GET");
        sessionConn.setConnectTimeout(3000);
        sessionConn.setReadTimeout(3000);

        if (sessionConn.getResponseCode() != 200) {
            throw new Exception("Could not fetch skin data for: " + playerName);
        }

        BufferedReader sessionReader = new BufferedReader(new InputStreamReader(sessionConn.getInputStream()));
        JsonObject sessionObj = new JsonParser().parse(sessionReader).getAsJsonObject();
        JsonArray properties = sessionObj.getAsJsonArray("properties");
        JsonObject skinProperty = properties.get(0).getAsJsonObject();

        String value = skinProperty.get("value").getAsString();
        String signature = skinProperty.get("signature").getAsString();

        return new SkinProperty(value, signature);
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