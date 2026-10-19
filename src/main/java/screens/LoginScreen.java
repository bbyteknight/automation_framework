package screens;

import base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Login screen of Sauce Labs' "My Demo App", reached via the drawer menu's "Log In" item.
 * Cross-platform screen object: each field carries both an @AndroidFindBy and an
 * @iOSXCUITFindBy, and AppiumFieldDecorator (wired in BaseScreen) resolves whichever
 * matches the driver's actual platform — the screen class itself has no platform branching.
 *
 * Android locators were captured from the live app via `adb shell uiautomator dump` and are
 * verified. This app doesn't expose accessibility ids (content-desc) on the username/password
 * input fields themselves — only resource-ids — so those two fields fall back to @AndroidFindBy(id=...)
 * per the app's real accessibility tree; the login button does have a real accessibility id and
 * uses it. The @iOSXCUITFindBy values are placeholders mirroring the same identifiers, since
 * there is no iOS/macOS environment available here to verify them against a real build — they
 * will need confirming against an actual iOS app before being relied on.
 */
public class LoginScreen extends BaseScreen {

    @AndroidFindBy(id = "nameET")
    @iOSXCUITFindBy(accessibility = "Username")
    private WebElement usernameField;

    @AndroidFindBy(id = "passwordET")
    @iOSXCUITFindBy(accessibility = "Password")
    private WebElement passwordField;

    @AndroidFindBy(accessibility = "Tap to login with given credentials")
    @iOSXCUITFindBy(accessibility = "Tap to login with given credentials")
    private WebElement loginButton;

    public LoginScreen(WebDriver driver) {
        super(driver);
    }

    public void enterUsername(String username) {
        type(usernameField, username);
    }

    public void enterPassword(String password) {
        type(passwordField, password);
    }

    public HomeScreen login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        tap(loginButton);
        return new HomeScreen(driver);
    }
}
