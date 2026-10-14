package utils;

import config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;


public class DriverFactory {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public static WebDriver initDriver(String browser) {
        browser = browser.toLowerCase().trim();
        switch (browser) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                if (ConfigReader.get("headless").equalsIgnoreCase("true"))
                    chromeOptions.addArguments("--headless=new");
                driver.set(new ChromeDriver(chromeOptions));
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--start-maximized");
                if (ConfigReader.get("headless").equalsIgnoreCase("true"))
                    firefoxOptions.addArguments("--headless");
                driver.set(new FirefoxDriver(firefoxOptions));
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                if (ConfigReader.get("headless").equalsIgnoreCase("true"))
                    edgeOptions.addArguments("--headless=new");
                driver.set(new EdgeDriver(edgeOptions));
                break;
            default:
                throw new RuntimeException("❗ Browser not supported: " + browser);
        }
        return driver.get();
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

    public static void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
        }
    }
}
