package utils;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralised explicit-wait helpers. Every wait resolves its timeout from
 * ConfigReader ("explicitTimeout") unless an override is passed in, so page
 * objects never hardcode a duration. BasePage is built on top of this class.
 */
public class WaitUtils {

    private final WebDriver driver;

    public WaitUtils(WebDriver driver) {
        this.driver = driver;
    }

    private WebDriverWait wait(int timeoutSeconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
    }

    private int defaultTimeout() {
        return Integer.parseInt(ConfigReader.get("explicitTimeout"));
    }

    public WebElement waitForVisibility(WebElement element) {
        return waitForVisibility(element, defaultTimeout());
    }

    public WebElement waitForVisibility(WebElement element, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.visibilityOf(element));
    }

    public WebElement waitForVisibility(By locator) {
        return waitForVisibility(locator, defaultTimeout());
    }

    public WebElement waitForVisibility(By locator, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickability(WebElement element) {
        return waitForClickability(element, defaultTimeout());
    }

    public WebElement waitForClickability(WebElement element, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(element));
    }

    public WebElement waitForClickability(By locator) {
        return waitForClickability(locator, defaultTimeout());
    }

    public WebElement waitForClickability(By locator, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public boolean waitForInvisibility(WebElement element) {
        return waitForInvisibility(element, defaultTimeout());
    }

    public boolean waitForInvisibility(WebElement element, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.invisibilityOf(element));
    }

    public boolean waitForInvisibility(By locator) {
        return waitForInvisibility(locator, defaultTimeout());
    }

    public boolean waitForInvisibility(By locator, int timeoutSeconds) {
        return wait(timeoutSeconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }
}
