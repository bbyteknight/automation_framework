package utils;

import config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Thread-safe web driver factory: each thread gets its own WebDriver instance
 * via ThreadLocal, so parallel TestNG/Cucumber execution (see testng.xml
 * parallel="methods") never shares a driver across threads.
 *
 * Driver creation branches on config, not on which method is called:
 *  - executionMode=local (default) -> local ChromeDriver/FirefoxDriver/EdgeDriver,
 *    binaries resolved via WebDriverManager.
 *  - executionMode=grid|cloud      -> RemoteWebDriver against gridUrl, using the
 *    same per-browser Options as capabilities (works for a local Selenium Grid
 *    and for cloud providers that speak the standard WebDriver protocol).
 *
 * drivers.MobileDriverFactory registers Appium drivers into this same
 * ThreadLocal via setDriver(), so hooks/steps can use getDriver()/quitDriver()
 * uniformly regardless of platform.
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver initDriver(String browser) {
        browser = browser.toLowerCase().trim();
        String executionMode = ConfigReader.get("executionMode", "local").toLowerCase().trim();
        boolean headless = ConfigReader.get("headless", "false").equalsIgnoreCase("true");

        // "remote" is kept as an alias for "grid" since qa/staging.properties already
        // ship with executionMode=remote.
        WebDriver webDriver = switch (executionMode) {
            case "grid", "cloud", "remote" -> createRemoteDriver(browser, headless);
            default -> createLocalDriver(browser, headless);
        };

        driver.set(webDriver);
        return webDriver;
    }

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        return switch (browser) {
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(chromeOptions(headless));
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                yield new FirefoxDriver(firefoxOptions(headless));
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                yield new EdgeDriver(edgeOptions(headless));
            }
            default -> throw new RuntimeException("❗ Browser not supported: " + browser);
        };
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless) {
        String gridUrl = ConfigReader.get("gridUrl", "http://localhost:4444/wd/hub");
        MutableCapabilities options = switch (browser) {
            case "chrome" -> chromeOptions(headless);
            case "firefox" -> firefoxOptions(headless);
            case "edge" -> edgeOptions(headless);
            default -> throw new RuntimeException("❗ Browser not supported: " + browser);
        };

        String platformName = ConfigReader.get("platformName", "");
        if (!platformName.isBlank()) {
            options.setCapability("platformName", platformName);
        }

        return new RemoteWebDriver(toUrl(gridUrl), (Capabilities) options);
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        // Safe on bare metal too; required in containers, where /dev/shm is
        // small by default and chrome otherwise often crashes mid-run.
        options.addArguments("--disable-dev-shm-usage");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--start-maximized");
        if (headless) {
            options.addArguments("--headless");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-dev-shm-usage");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new RuntimeException("❗ Invalid grid/cloud URL: " + url, e);
        }
    }

    public static void launchApp() {
        driver.get().get(ConfigReader.get("baseUrl"));
    }

    public static WebDriver getDriver() {
        return driver.get();
    }

    /**
     * Lets other driver factories (e.g. drivers.MobileDriverFactory for Appium)
     * register their driver under the same ThreadLocal, so hooks/steps can use
     * DriverFactory.getDriver()/quitDriver() uniformly regardless of platform.
     */
    public static void setDriver(WebDriver webDriver) {
        driver.set(webDriver);
    }

    /** Quits the current thread's driver (if any) and always clears the ThreadLocal, even if quit() throws. */
    public static void quitDriver() {
        WebDriver current = driver.get();
        if (current == null) {
            return;
        }
        try {
            current.quit();
        } finally {
            driver.remove();
        }
    }
}
