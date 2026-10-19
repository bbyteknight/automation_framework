package base;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;
import utils.WaitUtils;

/**
 * Common, stable browser actions shared by every page object. Page objects
 * extend this instead of each wiring their own WebDriverWait/JavascriptExecutor/
 * Actions boilerplate.
 *
 * Every action here waits for the element to be in the right state first
 * (visible for reads, clickable for clicks) instead of a fixed pause, so page
 * objects never need Thread.sleep — see the project's Checkstyle rule, which
 * fails the build on any Thread.sleep() call.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WaitUtils wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(driver, this);
    }

    // ---- Core actions ----

    protected void click(WebElement element) {
        wait.waitForClickability(element).click();
    }

    protected void click(By locator) {
        wait.waitForClickability(locator).click();
    }

    protected void type(WebElement element, String text) {
        WebElement target = wait.waitForVisibility(element);
        target.clear();
        target.sendKeys(text);
    }

    protected void type(By locator, String text) {
        WebElement target = wait.waitForVisibility(locator);
        target.clear();
        target.sendKeys(text);
    }

    protected String getText(WebElement element) {
        return wait.waitForVisibility(element).getText();
    }

    protected String getText(By locator) {
        return wait.waitForVisibility(locator).getText();
    }

    protected void selectByVisibleText(WebElement dropdown, String visibleText) {
        new Select(wait.waitForVisibility(dropdown)).selectByVisibleText(visibleText);
    }

    protected void selectByValue(WebElement dropdown, String value) {
        new Select(wait.waitForVisibility(dropdown)).selectByValue(value);
    }

    protected void selectByIndex(WebElement dropdown, int index) {
        new Select(wait.waitForVisibility(dropdown)).selectByIndex(index);
    }

    protected void hover(WebElement element) {
        new Actions(driver).moveToElement(wait.waitForVisibility(element)).perform();
    }

    protected void scrollIntoView(WebElement element) {
        executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    protected void scrollBy(int xPixels, int yPixels) {
        executeScript("window.scrollBy(arguments[0], arguments[1]);", xPixels, yPixels);
    }

    // ---- JavaScript executor helpers ----

    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /** Fallback click for elements a native click can't reach (e.g. covered by a sticky header). */
    protected void jsClick(WebElement element) {
        executeScript("arguments[0].click();", element);
    }

    // ---- Shadow DOM ----

    /** Returns the open shadow root of the given host element as a SearchContext usable with findElement/findElements. */
    protected SearchContext expandShadowRoot(WebElement shadowHost) {
        return (SearchContext) executeScript("return arguments[0].shadowRoot;", shadowHost);
    }

    /** Finds a single element inside the given shadow host's shadow DOM. */
    protected WebElement findInShadow(WebElement shadowHost, By locator) {
        return expandShadowRoot(shadowHost).findElement(locator);
    }
}
