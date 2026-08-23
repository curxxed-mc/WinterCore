package net.curxxed.dev.wintercore.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class PluginMessageData {

    private PluginMessageData() {
        throw new UnsupportedOperationException();
    }

    public static byte[] encode(String... values) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            for (String value : values) {
                output.writeUTF(value == null ? "" : value);
            }
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not encode plugin message", impossible);
        }
        return bytes.toByteArray();
    }
}
