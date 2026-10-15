package api.models;

import java.time.Duration;
import java.time.Instant;

public class AuthToken {

    // Treat a token as expired slightly before its real expiry so an in-flight
    // request never gets a token that dies mid-call.
    private static final Duration REFRESH_BUFFER = Duration.ofSeconds(30);

    private final String accessToken;
    private final Instant expiresAt;

    private AuthToken(String accessToken, Instant expiresAt) {
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public static AuthToken nonExpiring(String accessToken) {
        return new AuthToken(accessToken, null);
    }

    public static AuthToken expiringIn(String accessToken, long expiresInSeconds) {
        return new AuthToken(accessToken, Instant.now().plusSeconds(expiresInSeconds));
    }

    public String getAccessToken() {
        return accessToken;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt.minus(REFRESH_BUFFER));
    }
}
