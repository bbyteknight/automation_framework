package base;

import config.ConfigReader;
import io.qameta.allure.testng.AllureTestNg;
import listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import utils.DriverFactory;

// Scoped here (not suite-wide in testng.xml) so it only wraps plain TestNG tests like
// LoginTest — the Cucumber runner already gets its own, correctly-structured Allure result via
// the AllureCucumber7Jvm Cucumber plugin, and suite-wide AllureTestNg would double-report it.
@Listeners({TestListener.class, AllureTestNg.class})
public class BaseTest {

    protected WebDriver driver;

    @Parameters({"browser"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("chrome") String browser) {
        ConfigReader.loadProperties();
        driver = DriverFactory.initDriver(browser);
        DriverFactory.launchApp();
        System.out.println("✅ " + browser + " browser started successfully.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    public WebDriver getDriver() {
        return driver;
    }
}
