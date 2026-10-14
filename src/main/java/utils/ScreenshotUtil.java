package utils;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtil {


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
}
