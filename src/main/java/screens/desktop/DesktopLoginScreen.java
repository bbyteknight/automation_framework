package screens.desktop;

import base.DesktopBaseScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Login view of the sample TestLoginApp (WinForms, Card 35) — see its own
 * README.md (TestLoginApp/README.md) for the full AutomationId/locator table.
 * Template for any future Windows desktop screen object: locate everything by
 * AutomationId via DesktopBaseScreen#automationId(String), never by index/xpath.
 */
public class DesktopLoginScreen extends DesktopBaseScreen {

    private final By usernameField = automationId("txtUsername");
    private final By passwordField = automationId("txtPassword");
    private final By loginButton = automationId("btnLogin");
    private final By errorMessage = automationId("lblError");

    public DesktopLoginScreen(WebDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return isDisplayed(usernameField);
    }

    public void enterUsername(String username) {
        type(usernameField, username);
    }

    public void enterPassword(String password) {
        type(passwordField, password);
    }

    public void submit() {
        click(loginButton);
    }

    /** Logs in and returns the Dashboard screen — only valid when the credentials are correct. */
    public DesktopDashboardScreen login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        submit();
        return new DesktopDashboardScreen(driver);
    }

    /** Submits with invalid credentials and stays on this screen so the error message can be asserted. */
    public DesktopLoginScreen loginExpectingError(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        submit();
        return this;
    }

    public boolean isErrorDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getErrorMessage() {
        return getText(errorMessage);
    }
}
