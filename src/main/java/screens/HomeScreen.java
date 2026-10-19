package screens;

import base.BaseScreen;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Home / Products catalog screen of Sauce Labs' "My Demo App" — the app's landing screen,
 * doubling as the post-login destination (there's no separate "home" activity; Login simply
 * returns here). Cross-platform screen object: see LoginScreen for the @AndroidFindBy /
 * @iOSXCUITFindBy pairing convention and its caveats.
 *
 * The drawer menu's "Log In" / "Log Out" item is the real, verified logged-in indicator for
 * this app (confirmed live: it reads "Log In" with accessibility id "Login Menu Item" when
 * logged out, and switches to "Log Out" / "Logout Menu Item" immediately after a successful
 * login) — there is no separate profile/welcome element.
 */
public class HomeScreen extends BaseScreen {

    @AndroidFindBy(accessibility = "View menu")
    @iOSXCUITFindBy(accessibility = "View menu")
    private WebElement menuIcon;

    @AndroidFindBy(accessibility = "Login Menu Item")
    @iOSXCUITFindBy(accessibility = "Login Menu Item")
    private WebElement loginMenuItem;

    @AndroidFindBy(accessibility = "Logout Menu Item")
    @iOSXCUITFindBy(accessibility = "Logout Menu Item")
    private WebElement logoutMenuItem;

    public HomeScreen(WebDriver driver) {
        super(driver);
    }

    public void openMenu() {
        tap(menuIcon);
    }

    public LoginScreen openLoginScreen() {
        openMenu();
        tap(loginMenuItem);
        return new LoginScreen(driver);
    }

    /** True once the drawer menu shows "Log Out" instead of "Log In" (menu must already be open). */
    public boolean isLoggedIn() {
        return wait.isDisplayed(logoutMenuItem);
    }
}
