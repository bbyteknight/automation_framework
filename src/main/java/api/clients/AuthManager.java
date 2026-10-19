package api.clients;

import api.models.AuthToken;
import config.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Centralised API auth: caches a single token for the JVM and refreshes it
 * only when it's missing or about to expire, instead of every test fetching
 * its own.
 *
 * Two modes, picked automatically from config:
 *  - authUrl not set  -> static token mode: uses apiToken as-is, never expires.
 *  - authUrl set      -> OAuth2 client_credentials grant against authUrl using
 *                        apiClientId / apiClientSecret, token cached until
 *                        expires_in (minus a refresh buffer) elapses.
 */
public final class AuthManager {

    private static final Logger logger = LogManager.getLogger(AuthManager.class);

    private static volatile AuthToken cachedToken;

    private AuthManager() {
    }

    public static synchronized String getAccessToken() {
        if (cachedToken == null || cachedToken.isExpired()) {
            cachedToken = fetchToken();
        }
        return cachedToken.getAccessToken();
    }

    /** Forces the next call to getAccessToken() to fetch a fresh token. */
    public static synchronized void invalidate() {
        cachedToken = null;
    }

    private static AuthToken fetchToken() {
        String authUrl = ConfigReader.get("authUrl", "");

        if (authUrl.isBlank()) {
            return staticToken();
        }
        return oauthClientCredentialsToken(authUrl);
    }

    private static AuthToken staticToken() {
        String apiToken = ConfigReader.get("apiToken", "");
        if (apiToken.isBlank()) {
            throw new IllegalStateException(
                    "No API auth configured — set apiToken (static token) or authUrl + "
                            + "apiClientId + apiClientSecret (OAuth2 client_credentials).");
        }
        logger.debug("Using static apiToken for API authentication.");
        return AuthToken.nonExpiring(apiToken);
    }

    private static AuthToken oauthClientCredentialsToken(String authUrl) {
        String clientId = ConfigReader.get("apiClientId", "");
        String clientSecret = ConfigReader.get("apiClientSecret", "");

        logger.info("Requesting OAuth2 access token from {} (client_credentials grant).", authUrl);

        Response response = RestAssured.given()
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret)
                .post(authUrl);

        response.then().statusCode(200);

        String accessToken = response.jsonPath().getString("access_token");
        long expiresIn = response.jsonPath().getLong("expires_in");

        logger.info("OAuth2 token acquired, expires in {}s.", expiresIn);
        return AuthToken.expiringIn(accessToken, expiresIn);
    }
}
