package utils;

import config.ConfigReader;
import io.appium.java_client.windows.WindowsDriver;
import io.appium.java_client.windows.options.WindowsOptions;
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
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
 *
 * Desktop (Windows) scenarios are handled directly here via initDesktopDriver(),
 * using Appium's NovaWindows driver (see DESKTOP_AUTOMATION_SETUP.md for the
 * one-time Appium/driver install) — same ThreadLocal, same getDriver()/quitDriver().
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final String LOCAL_APPIUM_URL = "http://127.0.0.1:4723";

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
                ChromeOptions options = chromeOptions(headless);
                applyLocalDownloadPrefs(options);
                yield new ChromeDriver(options);
            }
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = firefoxOptions(headless);
                applyLocalDownloadPrefs(options);
                yield new FirefoxDriver(options);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions options = edgeOptions(headless);
                applyLocalDownloadPrefs(options);
                yield new EdgeDriver(options);
            }
            default -> throw new RuntimeException("❗ Browser not supported: " + browser);
        };
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless) {
        String gridUrl = ConfigReader.get("gridUrl", "http://localhost:4444/wd/hub");
        // Grid (and any cloud provider speaking the same WebDriver protocol extension) exposes
        // HasDownloads once se:downloadsEnabled is on — see utils.DownloadUtil.
        MutableCapabilities options = switch (browser) {
            case "chrome" -> enableRemoteDownloads(chromeOptions(headless));
            case "firefox" -> enableRemoteDownloads(firefoxOptions(headless));
            case "edge" -> enableRemoteDownloads(edgeOptions(headless));
            default -> throw new RuntimeException("❗ Browser not supported: " + browser);
        };

        String platformName = ConfigReader.get("platformName", "");
        if (!platformName.isBlank()) {
            options.setCapability("platformName", platformName);
        }

        RemoteWebDriver remoteDriver = new RemoteWebDriver(toUrl(gridUrl), (Capabilities) options);
        // Without this, sendKeys(localPath) on a file input looks for that path on the Grid node
        // itself and fails — LocalFileDetector instead zips the local file and ships it to the
        // node transparently, so upload steps (see pages.UploadPage) need no local/remote branch.
        remoteDriver.setFileDetector(new LocalFileDetector());
        return remoteDriver;
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

    /** Local-run downloads land straight in {@link DownloadUtil#downloadDir()} — no prompt, no .crdownload left unresolved. */
    private static void applyLocalDownloadPrefs(ChromeOptions options) {
        options.setExperimentalOption("prefs", chromiumDownloadPrefs());
    }

    private static void applyLocalDownloadPrefs(EdgeOptions options) {
        options.setExperimentalOption("prefs", chromiumDownloadPrefs());
    }

    private static void applyLocalDownloadPrefs(FirefoxOptions options) {
        String downloadDir = DownloadUtil.downloadDir().toAbsolutePath().toString();
        options.addPreference("browser.download.folderList", 2);
        options.addPreference("browser.download.dir", downloadDir);
        options.addPreference("browser.download.useDownloadDir", true);
        options.addPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,application/zip,application/pdf,text/csv,text/plain,"
                        + "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        options.addPreference("pdfjs.disabled", true);
    }

    private static Map<String, Object> chromiumDownloadPrefs() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", DownloadUtil.downloadDir().toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        return prefs;
    }

    private static <T extends AbstractDriverOptions<?>> T enableRemoteDownloads(T options) {
        options.setEnableDownloads(true);
        return options;
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new RuntimeException("❗ Invalid grid/cloud URL: " + url, e);
        }
    }

    /**
     * Starts a desktop driver for @desktop scenarios. Only "windows" (Appium's NovaWindows
     * driver) is supported today; the platform is config-driven (-Dplatform=windows, or
     * PLATFORM/.env/properties file) rather than hardcoded, so another desktop OS can be
     * added here later without touching Hooks.
     *
     * desktopAppPath accepts anything NovaWindows' "app" capability does: a .exe path for a
     * classic Win32/WinForms/WPF app, or a UWP AUMID (e.g.
     * "Microsoft.WindowsCalculator_8wekyb3d8bbwe!App").
     */
    public static WebDriver initDesktopDriver() {
        String platform = ConfigReader.get("platform", "windows").toLowerCase().trim();

        WebDriver desktopDriver = switch (platform) {
            case "windows" -> createWindowsDriver();
            default -> throw new RuntimeException("❗ Desktop platform not supported: " + platform);
        };

        driver.set(desktopDriver);
        return desktopDriver;
    }

    private static WebDriver createWindowsDriver() {
        URL appiumServerUrl = toUrl(ConfigReader.get("appiumServerUrl", LOCAL_APPIUM_URL));

        WindowsOptions options = new WindowsOptions();
        options.setPlatformName("Windows");
        options.setAutomationName("NovaWindows");
        setIfPresent("desktopAppPath", options::setApp);
        setIfPresent("desktopAppArguments", options::setAppArguments);
        setIfPresent("desktopAppWorkingDir", options::setAppWorkingDir);

        // NovaWindows' PowerShell-backed automation can otherwise click/type before the target
        // control has actually taken focus (observed as a click landing but not registering, or
        // text going to the wrong field) — these are the driver's own documented capabilities
        // for that, not exposed via a typed WindowsOptions setter in this java-client version.
        options.setCapability("delayBeforeClick", Integer.parseInt(ConfigReader.get("desktopDelayBeforeClick", "150")));
        options.setCapability("delayAfterClick", Integer.parseInt(ConfigReader.get("desktopDelayAfterClick", "150")));

        return new WindowsDriver(appiumServerUrl, options);
    }

    private static void setIfPresent(String configKey, Consumer<String> setter) {
        String value = ConfigReader.get(configKey, "");
        if (!value.isBlank()) {
            setter.accept(value);
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
