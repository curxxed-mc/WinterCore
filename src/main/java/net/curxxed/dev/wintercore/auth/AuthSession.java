package net.curxxed.dev.wintercore.auth;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AuthSession {

    private final UUID playerUUID;
    private final String ipAddress;
    private final long expiresAt;

    public AuthSession(UUID playerUUID, String ipAddress, long expiresAt) {
        this.playerUUID = playerUUID;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
    }

    public boolean isValid(String currentIp) {
        return System.currentTimeMillis() < expiresAt && ipAddress.equals(currentIp);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}