package drivers;

import config.ConfigReader;
import exceptions.ConfigurationException;
import exceptions.FrameworkException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import utils.RetryHelper;

import java.io.File;
import java.time.Duration;

/**
 * Uploads a local .apk/.ipa to BrowserStack App Automate so it can be referenced by the
 * "bs://&lt;hash&gt;" app URL BrowserStack's cloud capabilities expect, instead of a local file
 * path (which only makes sense for a local Appium server). Used automatically by
 * MobileDriverFactory when executionMode=cloud and an appPath is configured but no appUrl
 * override is already set.
 *
 * Credentials come from ConfigReader ("browserstackUsername" / "browserstackAccessKey"), which
 * per ConfigReader's resolution order are expected to be supplied via the BROWSERSTACK_USERNAME /
 * BROWSERSTACK_ACCESS_KEY environment variables (CI secret store or a local, gitignored .env
 * file) — never hardcoded or committed.
 */
public class CloudAppUploader {

    private static final String UPLOAD_URL = "https://api-cloud.browserstack.com/app-automate/upload";

    private CloudAppUploader() {
    }

    /** Uploads the given local app file and returns the resulting "bs://..." app URL. */
    public static String uploadApp(String localAppPath) {
        String username = requireCredential("browserstackUsername");
        String accessKey = requireCredential("browserstackAccessKey");

        File appFile = new File(localAppPath);
        if (!appFile.isFile()) {
            throw new FrameworkException("❗ App file not found for BrowserStack upload: " + localAppPath);
        }

        // Transient network blips against BrowserStack's upload endpoint are infra-level
        // flakiness, not a problem with the app file — worth a couple of retries before giving up.
        Response response = RetryHelper.retry(
                () -> RestAssured.given()
                        .auth().preemptive().basic(username, accessKey)
                        .multiPart("file", appFile)
                        .when()
                        .post(UPLOAD_URL),
                3, Duration.ofSeconds(2));

        if (response.statusCode() != 200) {
            throw new FrameworkException("❗ BrowserStack app upload failed (HTTP " + response.statusCode()
                    + "): " + response.getBody().asString());
        }

        String appUrl = response.jsonPath().getString("app_url");
        if (appUrl == null || appUrl.isBlank()) {
            throw new FrameworkException("❗ BrowserStack app upload response missing app_url: "
                    + response.getBody().asString());
        }
        return appUrl;
    }

    private static String requireCredential(String key) {
        String value = ConfigReader.get(key, "");
        if (value.isBlank()) {
            throw new ConfigurationException("❗ Missing BrowserStack credential \"" + key + "\" — set it via the "
                    + key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase() + " environment variable.");
        }
        return value;
    }
}
