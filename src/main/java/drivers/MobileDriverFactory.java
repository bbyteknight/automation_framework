package drivers;

import config.ConfigReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.remote.options.BaseOptions;
import io.appium.java_client.remote.options.SupportsAppOption;
import org.openqa.selenium.WebDriver;
import utils.DriverFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Thread-safe mobile driver factory for Appium 2 (UiAutomator2 for Android,
 * XCUITest for iOS). Device capabilities are entirely config-driven via
 * ConfigReader, so the same code targets an emulator/simulator, a local real
 * device, or a BrowserStack cloud real device depending only on executionMode
 * and what's set in the environment properties file (or a -D/env override) —
 * nothing here hardcodes a specific device.
 *
 * executionMode:
 *  - "local" (default) -> connects to a local Appium server (appiumServerUrl),
 *                          targeting an emulator/simulator or a locally
 *                          attached real device.
 *  - "cloud"           -> connects to BrowserStack's Appium hub using
 *                          BROWSERSTACK_USERNAME / BROWSERSTACK_ACCESS_KEY
 *                          (env vars — never hardcoded), running on a real
 *                          device in BrowserStack's device farm. If appPath
 *                          points at a local file, it's uploaded automatically
 *                          via CloudAppUploader; set appUrl instead to reuse
 *                          an app already uploaded to BrowserStack.
 *
 * Emulator/simulator vs local real device (when executionMode=local) is just
 * a matter of which capabilities are populated:
 *  - avd (Android only)   -> Appium boots/targets a named emulator
 *  - udid (Android & iOS) -> targets an already-running emulator/simulator
 *                            or a real device's serial/UDID
 *  - deviceName + platformVersion alone -> Appium picks a matching
 *                            simulator (iOS) or relies on a single already
 *                            connected/running device (Android)
 *
 * Every capability setter below is skipped when the config value is blank,
 * so unset keys never reach Appium as empty-string capabilities (which
 * Appium/the underlying driver can reject outright).
 *
 * Registers the created driver into DriverFactory's ThreadLocal via
 * setDriver(), so hooks/steps use DriverFactory.getDriver()/quitDriver()
 * uniformly regardless of platform or execution mode.
 */
public class MobileDriverFactory {

    private static final String LOCAL_APPIUM_URL = "http://127.0.0.1:4723";
    private static final String BROWSERSTACK_HUB_HOST = "hub-cloud.browserstack.com/wd/hub";

    public static WebDriver initDriver(String platform) {
        platform = platform.toLowerCase().trim();
        String executionMode = ConfigReader.get("executionMode", "local").toLowerCase();

        WebDriver mobileDriver = "cloud".equals(executionMode)
                ? initCloudDriver(platform)
                : initLocalDriver(platform);

        DriverFactory.setDriver(mobileDriver);
        return mobileDriver;
    }

    private static WebDriver initLocalDriver(String platform) {
        URL appiumServerUrl = toUrl(ConfigReader.get("appiumServerUrl", LOCAL_APPIUM_URL));
        return switch (platform) {
            case "android" -> {
                UiAutomator2Options options = androidOptions();
                applyAppCapability(options, "local");
                yield new AndroidDriver(appiumServerUrl, options);
            }
            case "ios" -> {
                XCUITestOptions options = iosOptions();
                applyAppCapability(options, "local");
                yield new IOSDriver(appiumServerUrl, options);
            }
            default -> throw new RuntimeException("❗ Mobile platform not supported: " + platform);
        };
    }

    private static WebDriver initCloudDriver(String platform) {
        String username = requireCloudCredential("browserstackUsername");
        String accessKey = requireCloudCredential("browserstackAccessKey");
        URL hubUrl = toUrl("https://" + username + ":" + accessKey + "@" + BROWSERSTACK_HUB_HOST);

        return switch (platform) {
            case "android" -> {
                UiAutomator2Options options = androidOptions();
                applyAppCapability(options, "cloud");
                options.setCapability("bstack:options", browserStackOptions());
                yield new AndroidDriver(hubUrl, options);
            }
            case "ios" -> {
                XCUITestOptions options = iosOptions();
                applyAppCapability(options, "cloud");
                options.setCapability("bstack:options", browserStackOptions());
                yield new IOSDriver(hubUrl, options);
            }
            default -> throw new RuntimeException("❗ Mobile platform not supported: " + platform);
        };
    }

    /** BrowserStack's vendor capability block — real-device selection, build grouping, and diagnostics. */
    private static Map<String, Object> browserStackOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        putIfPresent(options, "deviceName", "deviceName");
        putIfPresent(options, "osVersion", "platformVersion");
        options.put("projectName", ConfigReader.get("cloudProjectName", "AutomationFramework"));
        options.put("buildName", ConfigReader.get("cloudBuildName", "local-build"));
        options.put("sessionName", ConfigReader.get("cloudSessionName", "Mobile session"));
        options.put("debug", true);
        options.put("networkLogs", true);
        return options;
    }

    private static void putIfPresent(Map<String, Object> target, String bstackKey, String configKey) {
        String value = ConfigReader.get(configKey, "");
        if (!value.isBlank()) {
            target.put(bstackKey, value);
        }
    }

    private static UiAutomator2Options androidOptions() {
        UiAutomator2Options options = new UiAutomator2Options();
        setIfPresent("deviceName", options::setDeviceName);
        setIfPresent("platformVersion", options::setPlatformVersion);
        setIfPresent("udid", options::setUdid);
        setIfPresent("avd", options::setAvd);
        setIfPresent("appPackage", options::setAppPackage);
        setIfPresent("appActivity", options::setAppActivity);
        return options;
    }

    private static XCUITestOptions iosOptions() {
        XCUITestOptions options = new XCUITestOptions();
        setIfPresent("deviceName", options::setDeviceName);
        setIfPresent("platformVersion", options::setPlatformVersion);
        setIfPresent("udid", options::setUdid);
        setIfPresent("bundleId", options::setBundleId);
        return options;
    }

    /**
     * Resolves the "app" capability for either execution mode: an explicit appUrl (e.g. an
     * already-uploaded "bs://..." BrowserStack app) always wins; otherwise a configured appPath
     * is used as-is for local runs, or uploaded to BrowserStack automatically for cloud runs.
     * appPackage/appActivity or bundleId alone (app already installed, no appPath/appUrl) is
     * also valid and simply leaves this capability unset.
     */
    private static <T extends BaseOptions<T> & SupportsAppOption<T>> void applyAppCapability(
            T options, String executionMode) {
        String appUrl = ConfigReader.get("appUrl", "");
        String appPath = ConfigReader.get("appPath", "");

        if (!appUrl.isBlank()) {
            options.setApp(appUrl);
        } else if (!appPath.isBlank()) {
            options.setApp("cloud".equals(executionMode) ? CloudAppUploader.uploadApp(appPath) : appPath);
        }
    }

    private static void setIfPresent(String configKey, Consumer<String> setter) {
        String value = ConfigReader.get(configKey, "");
        if (!value.isBlank()) {
            setter.accept(value);
        }
    }

    private static String requireCloudCredential(String key) {
        String value = ConfigReader.get(key, "");
        if (value.isBlank()) {
            throw new RuntimeException("❗ executionMode=cloud requires the "
                    + key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase() + " environment variable.");
        }
        return value;
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new RuntimeException("❗ Invalid Appium server URL: " + url, e);
        }
    }
}
