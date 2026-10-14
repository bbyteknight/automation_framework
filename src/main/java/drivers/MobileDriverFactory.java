package drivers;

import config.ConfigReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.WebDriver;
import utils.DriverFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class MobileDriverFactory {

    public static WebDriver initDriver(String platform) {
        platform = platform.toLowerCase().trim();
        URL appiumServerUrl = toUrl(ConfigReader.get("appiumServerUrl", "http://127.0.0.1:4723"));

        WebDriver mobileDriver = switch (platform) {
            case "android" -> {
                UiAutomator2Options options = new UiAutomator2Options()
                        .setDeviceName(ConfigReader.get("deviceName", ""))
                        .setPlatformVersion(ConfigReader.get("platformVersion", ""))
                        .setAppPackage(ConfigReader.get("appPackage", ""))
                        .setAppActivity(ConfigReader.get("appActivity", ""));
                yield new AndroidDriver(appiumServerUrl, options);
            }
            case "ios" -> {
                XCUITestOptions options = new XCUITestOptions()
                        .setDeviceName(ConfigReader.get("deviceName", ""))
                        .setPlatformVersion(ConfigReader.get("platformVersion", ""))
                        .setBundleId(ConfigReader.get("bundleId", ""));
                yield new IOSDriver(appiumServerUrl, options);
            }
            default -> throw new RuntimeException("❗ Mobile platform not supported: " + platform);
        };

        DriverFactory.setDriver(mobileDriver);
        return mobileDriver;
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new RuntimeException("❗ Invalid Appium server URL: " + url, e);
        }
    }
}
