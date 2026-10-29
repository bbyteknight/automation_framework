package base;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import utils.WaitUtils;

import java.util.Set;

/**
 * Common, stable Windows desktop actions shared by every desktop screen object
 * (the desktop equivalent of BasePage/BaseScreen), built on top of Appium's
 * NovaWindows driver (see DESKTOP_AUTOMATION_SETUP.md for the one-time setup).
 *
 * There's no Windows-specific AppiumFieldDecorator annotation (unlike
 * @AndroidFindBy/@iOSXCUITFindBy for mobile), so desktop screens locate
 * elements via plain By locators rather than annotated WebElement fields —
 * see {@link #automationId(String)}, the primary locator strategy for this
 * driver: every control's Name in the sample TestLoginApp (and any WinForms/
 * WPF/UWP app under test) is exposed to UI Automation as its AutomationId.
 */
public abstract class DesktopBaseScreen {

    protected final WebDriver driver;
    protected final WaitUtils wait;

    protected DesktopBaseScreen(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    /** AutomationId -> accessibility id locator, the primary (and preferred) way to find desktop controls. */
    protected By automationId(String automationId) {
        return AppiumBy.accessibilityId(automationId);
    }

    // ---- Core actions ----

    protected void click(By locator) {
        wait.waitForClickability(locator).click();
    }

    protected void type(By locator, String text) {
        WebElement target = wait.waitForVisibility(locator);
        // NovaWindows' PowerShell-backed input needs the control to actually have focus before
        // clear()/sendKeys() — without this, keystrokes have landed on the previously-focused
        // control (or nowhere) instead of this one.
        target.click();
        target.clear();
        target.sendKeys(text);
    }

    protected String getText(By locator) {
        return wait.waitForVisibility(locator).getText();
    }

    /** Waits (up to the configured explicit timeout) for the element to appear, rather than an instant check. */
    protected boolean isDisplayed(By locator) {
        try {
            return wait.waitForVisibility(locator).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected WebElement waitForVisibility(By locator) {
        return wait.waitForVisibility(locator);
    }

    protected WebElement waitForClickability(By locator) {
        return wait.waitForClickability(locator);
    }

    protected boolean waitForInvisibility(By locator) {
        return wait.waitForInvisibility(locator);
    }

    // ---- Window handling ----

    /** All open top-level window handles for the application under test. */
    protected Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    /** Switches the driver's focus to the given window handle (from getWindowHandles()). */
    protected void switchToWindow(String windowHandle) {
        driver.switchTo().window(windowHandle);
    }

    /** Switches focus to whichever open window's title matches, e.g. a dialog that just popped up. */
    protected void switchToWindowByTitle(String title) {
        for (String handle : getWindowHandles()) {
            driver.switchTo().window(handle);
            if (title.equals(driver.getTitle())) {
                return;
            }
        }
        throw new org.openqa.selenium.NoSuchWindowException("❗ No open window with title: " + title);
    }

    protected void maximizeWindow() {
        driver.manage().window().maximize();
    }

    /** Closes only the current window/handle — use quitDriver() (via Hooks) to end the whole session. */
    protected void closeCurrentWindow() {
        driver.close();
    }

    // ---- Keyboard shortcuts / modifier keys ----

    /** Sends a single key (e.g. Keys.ENTER, Keys.ESCAPE, Keys.TAB) without any modifier. */
    protected void pressKey(Keys key) {
        new Actions(driver).sendKeys(key).perform();
    }

    /** Holds one modifier (e.g. Keys.CONTROL) while sending a literal key, e.g. Ctrl+A, Ctrl+S. */
    protected void pressKeyCombo(Keys modifier, CharSequence key) {
        new Actions(driver).keyDown(modifier).sendKeys(key).keyUp(modifier).perform();
    }

    /** Holds two modifiers at once while sending a literal key, e.g. Ctrl+Shift+S. */
    protected void pressKeyCombo(Keys modifier1, Keys modifier2, CharSequence key) {
        new Actions(driver).keyDown(modifier1).keyDown(modifier2).sendKeys(key)
                .keyUp(modifier2).keyUp(modifier1).perform();
    }

    /** Alt+F4 — closes the focused window via its own keyboard shortcut rather than driver.close(). */
    protected void pressAltF4() {
        pressKeyCombo(Keys.ALT, Keys.F4);
    }
}
