package base;

import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.appium.java_client.remote.SupportsContextSwitching;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.PageFactory;
import utils.WaitUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Common, stable mobile actions shared by every screen object (the mobile
 * equivalent of BasePage). Gestures are built on raw W3C Actions
 * (PointerInput + Sequence) rather than the deprecated TouchAction API, so
 * they work against both AndroidDriver and IOSDriver via the plain
 * WebDriver/Interactive contract.
 *
 * Elements are initialised through Appium's own AppiumFieldDecorator (not plain
 * Selenium PageFactory), so screens can declare fields with @AndroidFindBy /
 * @iOSXCUITFindBy and have the right one resolve per platform. Screens that
 * locate everything via raw By locators in tap(By)/getText(By) (no annotated
 * fields) are unaffected either way.
 */
public abstract class BaseScreen {

    private static final String NATIVE_CONTEXT = "NATIVE_APP";
    private static final String WEBVIEW_CONTEXT_PREFIX = "WEBVIEW";

    protected final WebDriver driver;
    protected final WaitUtils wait;

    protected BaseScreen(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    // ---- Core actions ----

    protected void tap(WebElement element) {
        wait.waitForVisibility(element).click();
    }

    protected void tap(By locator) {
        wait.waitForVisibility(locator).click();
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

    // ---- Gestures (W3C Actions) ----

    /** Single-finger swipe between two screen points. */
    protected void swipe(int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        perform(swipe);
    }

    protected void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(x, (int) (size.getHeight() * 0.8), x, (int) (size.getHeight() * 0.2));
    }

    protected void swipeDown() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        swipe(x, (int) (size.getHeight() * 0.2), x, (int) (size.getHeight() * 0.8));
    }

    protected void swipeLeft() {
        Dimension size = driver.manage().window().getSize();
        int y = size.getHeight() / 2;
        swipe((int) (size.getWidth() * 0.8), y, (int) (size.getWidth() * 0.2), y);
    }

    protected void swipeRight() {
        Dimension size = driver.manage().window().getSize();
        int y = size.getHeight() / 2;
        swipe((int) (size.getWidth() * 0.2), y, (int) (size.getWidth() * 0.8), y);
    }

    /** Repeatedly swipes up (default direction) until the element is found or maxSwipes is reached. */
    protected WebElement scrollToElement(By locator, int maxSwipes) {
        for (int attempt = 0; attempt < maxSwipes; attempt++) {
            var found = driver.findElements(locator);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return found.get(0);
            }
            swipeUp();
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "❗ Element not found after " + maxSwipes + " swipes: " + locator);
    }

    /** Presses and holds an element for the given duration before releasing. */
    protected void longPress(WebElement element, Duration duration) {
        org.openqa.selenium.Point center = elementCenter(element);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence longPress = new Sequence(finger, 0);
        longPress.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.x, center.y));
        longPress.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        longPress.addAction(new org.openqa.selenium.interactions.Pause(finger, duration));
        longPress.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        perform(longPress);
    }

    /** Drags one element onto another using a single continuous pointer gesture. */
    protected void drag(WebElement source, WebElement target) {
        org.openqa.selenium.Point from = elementCenter(source);
        org.openqa.selenium.Point to = elementCenter(target);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 0);
        drag.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), from.x, from.y));
        drag.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        drag.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), to.x, to.y));
        drag.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        perform(drag);
    }

    private org.openqa.selenium.Point elementCenter(WebElement element) {
        var location = element.getLocation();
        var size = element.getSize();
        return new org.openqa.selenium.Point(location.x + size.width / 2, location.y + size.height / 2);
    }

    private void perform(Sequence sequence) {
        ((Interactive) driver).perform(Collections.singletonList(sequence));
    }

    // ---- Keyboard / app lifecycle ----

    protected void hideKeyboard() {
        ((HidesKeyboard) driver).hideKeyboard();
    }

    /** Sends the app to the background for the given duration (negative = indefinitely), then returns it to the foreground. */
    protected void backgroundApp(Duration duration) {
        ((InteractsWithApps) driver).runAppInBackground(duration);
    }

    /** Resets app state by terminating and relaunching it, since Appium 2 dropped the old one-shot resetApp command. */
    protected void resetApp(String appId) {
        InteractsWithApps appDriver = (InteractsWithApps) driver;
        appDriver.terminateApp(appId);
        appDriver.activateApp(appId);
    }

    // ---- Hybrid app context switching (NATIVE/WEBVIEW) ----

    protected Set<String> getContextHandles() {
        return ((SupportsContextSwitching) driver).getContextHandles();
    }

    protected String getCurrentContext() {
        return ((SupportsContextSwitching) driver).getContext();
    }

    protected void switchToContext(String contextName) {
        ((SupportsContextSwitching) driver).context(contextName);
    }

    protected void switchToNativeContext() {
        switchToContext(NATIVE_CONTEXT);
    }

    /** Switches to the first available WEBVIEW_* context (there's exactly one in most hybrid apps). */
    protected void switchToWebViewContext() {
        String webviewContext = getContextHandles().stream()
                .filter(context -> context.startsWith(WEBVIEW_CONTEXT_PREFIX))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("❗ No WEBVIEW context available"));
        switchToContext(webviewContext);
    }
}
