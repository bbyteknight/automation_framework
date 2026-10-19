package utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class ScreenshotUtil {

    private static final String SCREENSHOT_DIR = "test-output/screenshots/";

    public static void addScreenshotToReport(WebDriver driver, ExtentTest test, String message, String status) {
        String base64Screenshot = captureScreenshotBase64(driver);

        if (status.equalsIgnoreCase("pass")) {
            test.pass(message,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        } else if (status.equalsIgnoreCase("fail")) {
            test.fail(message,
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
        }
    }

    private static String captureScreenshotBase64(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }

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
