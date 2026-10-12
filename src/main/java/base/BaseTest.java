package base;

import config.ConfigReader;
import listeners.TestListener;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;
import utils.DriverFactory;

@Listeners(TestListener.class)
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
