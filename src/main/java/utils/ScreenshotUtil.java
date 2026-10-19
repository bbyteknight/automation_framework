package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class ScreenshotUtil {

    private static final String SCREENSHOT_DIR = "test-output/screenshots/";

    public static byte[] captureScreenshotBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /** Saves a screenshot to test-output/screenshots/ with a timestamped, filename-safe name.
     * Works for any driver (ChromeDriver, AndroidDriver, IOSDriver) since all implement
     * TakesScreenshot — same call for web and mobile. */
    public static File saveScreenshotToFile(WebDriver driver, String name) {
        byte[] bytes = captureScreenshotBytes(driver);
        String fileName = StringHelper.sanitizeForFilename(name) + "_" + DateHelper.timestamp() + ".png";
        return FileHelper.writeBytesToFile(bytes, SCREENSHOT_DIR + fileName);
    }
}
