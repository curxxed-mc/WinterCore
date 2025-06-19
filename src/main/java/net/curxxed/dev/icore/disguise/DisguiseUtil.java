package net.curxxed.dev.icore.disguise;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import com.google.gson.*;

public class DisguiseUtil
{
    public static JsonObject readData(final String uuid) {
        final StringBuilder builder = new StringBuilder();
        try {
            final URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("GET");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return new JsonParser().parse(builder.toString()).getAsJsonObject();
        }
        catch (final Exception e) {
            return null;
        }
    }

    public static String readUUID(final String uuid) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + uuid);
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        connection.setConnectTimeout(3000);
        connection.setRequestMethod("GET");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        final JsonObject object = new JsonParser().parse(builder.toString()).getAsJsonObject();
        return object.get("id").getAsString();
    }
}